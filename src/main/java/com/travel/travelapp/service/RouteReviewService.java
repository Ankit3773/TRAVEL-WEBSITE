package com.travel.travelapp.service;

import com.travel.travelapp.dto.CreateRouteReviewRequest;
import com.travel.travelapp.dto.RouteReviewOverviewResponse;
import com.travel.travelapp.dto.RouteReviewResponse;
import com.travel.travelapp.dto.RouteReviewSummaryResponse;
import com.travel.travelapp.entity.AppUser;
import com.travel.travelapp.entity.Booking;
import com.travel.travelapp.entity.RouteReview;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.exception.ResourceNotFoundException;
import com.travel.travelapp.repository.BookingRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.RouteReviewRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteReviewService {

    private final RouteReviewRepository routeReviewRepository;
    private final BookingRepository bookingRepository;
    private final RouteRepository routeRepository;
    private final AuthService authService;

    @Transactional
    public RouteReviewResponse createOrUpdateReview(CreateRouteReviewRequest request, String userEmail) {
        Booking booking = bookingRepository.findDetailedById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getBookedByUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to review this booking");
        }
        if (!booking.getBookingStatus().isBookedState()) {
            throw new BadRequestException("Only confirmed travel bookings can be reviewed");
        }
        if (booking.getTripSchedule().getTravelDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Review can be added after the travel date");
        }

        AppUser user = authService.getUserByEmail(userEmail);
        RouteReview review = routeReviewRepository.findByBookingId(booking.getId()).orElseGet(RouteReview::new);
        LocalDateTime now = LocalDateTime.now();

        if (review.getId() == null) {
            review.setBooking(booking);
            review.setRoute(booking.getTripSchedule().getRoute());
            review.setReviewedByUser(user);
            review.setCreatedAt(now);
        }

        review.setRating(request.getRating());
        review.setTitle(normalizeTitle(request.getTitle(), booking));
        review.setComment(request.getComment().trim());
        review.setUpdatedAt(now);

        return toResponse(routeReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<RouteReviewResponse> getHighlights(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 8));
        return routeReviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteReviewSummaryResponse getRouteReviewSummary(Long routeId, int limit) {
        routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        List<RouteReviewResponse> reviews = routeReviewRepository
                .findByRouteIdOrderByCreatedAtDesc(routeId, PageRequest.of(0, Math.max(1, Math.min(limit, 8))))
                .stream()
                .map(this::toResponse)
                .toList();

        RouteReviewOverviewResponse overview = getRouteReviewOverviews(List.of(routeId)).stream()
                .findFirst()
                .orElse(RouteReviewOverviewResponse.builder()
                        .routeId(routeId)
                        .averageRating(0.0)
                        .totalReviews(0L)
                        .build());

        return RouteReviewSummaryResponse.builder()
                .routeId(routeId)
                .averageRating(overview.getAverageRating())
                .totalReviews(overview.getTotalReviews())
                .reviews(reviews)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RouteReviewOverviewResponse> getRouteReviewOverviews(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return List.of();
        }
        return routeReviewRepository.summarizeByRouteIds(routeIds).stream()
                .map(summary -> RouteReviewOverviewResponse.builder()
                        .routeId(summary.getRouteId())
                        .averageRating(roundAverage(summary.getAverageRating()))
                        .totalReviews(summary.getReviewCount())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, RouteReview> getReviewByBookingIdMap(Collection<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Map.of();
        }
        return routeReviewRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(review -> review.getBooking().getId(), Function.identity()));
    }

    private RouteReviewResponse toResponse(RouteReview review) {
        return RouteReviewResponse.builder()
                .reviewId(review.getId())
                .routeId(review.getRoute().getId())
                .bookingId(review.getBooking().getId())
                .source(review.getRoute().getSource())
                .destination(review.getRoute().getDestination())
                .reviewerName(displayReviewerName(review))
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String displayReviewerName(RouteReview review) {
        String name = review.getReviewedByUser() != null ? review.getReviewedByUser().getName() : review.getBooking().getPassengerName();
        if (name == null || name.isBlank()) {
            return "Verified traveller";
        }
        String trimmed = name.trim();
        int firstSpace = trimmed.indexOf(' ');
        return firstSpace > 0 ? trimmed.substring(0, firstSpace) : trimmed;
    }

    private String normalizeTitle(String title, Booking booking) {
        String normalized = title == null ? "" : title.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return booking.getTripSchedule().getRoute().getSource() + " to "
                + booking.getTripSchedule().getRoute().getDestination();
    }

    private double roundAverage(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
