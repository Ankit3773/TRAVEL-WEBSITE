package com.travel.travelapp.controller;

import com.travel.travelapp.dto.CreateMarriageInquiryRequest;
import com.travel.travelapp.dto.MarriageInquiryResponse;
import com.travel.travelapp.entity.MarriageBookingInquiry;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.repository.MarriageBookingInquiryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Marriage Booking")
public class MarriageController {

    private static final int MAX_DISTANCE_KM = 175;
    private static final BigDecimal MIN_RATE_PER_KM = new BigDecimal("100");
    private static final BigDecimal MAX_RATE_PER_KM = new BigDecimal("180");
    private static final Set<Month> VALID_MONTHS = Set.of(
            Month.MARCH, Month.APRIL, Month.MAY, Month.JUNE,
            Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER);

    private final MarriageBookingInquiryRepository repository;

    @PostMapping("/marriage/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public MarriageInquiryResponse submitInquiry(@RequestBody CreateMarriageInquiryRequest request) {
        validate(request);

        MarriageBookingInquiry inquiry = new MarriageBookingInquiry();
        inquiry.setContactName(request.getContactName().trim());
        inquiry.setContactPhone(request.getContactPhone().trim());
        inquiry.setPickupLocation(request.getPickupLocation().trim());
        inquiry.setDropLocation(request.getDropLocation().trim());
        inquiry.setDistanceKm(request.getDistanceKm());
        inquiry.setWeddingDate(request.getWeddingDate());
        inquiry.setMessage(request.getMessage() != null ? request.getMessage().trim() : null);

        int km = request.getDistanceKm();
        inquiry.setEstimatedMinFare(MIN_RATE_PER_KM.multiply(BigDecimal.valueOf(km)));
        inquiry.setEstimatedMaxFare(MAX_RATE_PER_KM.multiply(BigDecimal.valueOf(km)));

        return toResponse(repository.save(inquiry));
    }

    @GetMapping("/admin/marriage/inquiries")
    public List<MarriageInquiryResponse> getAllInquiries() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validate(CreateMarriageInquiryRequest req) {
        if (req.getContactName() == null || req.getContactName().isBlank()) {
            throw new BadRequestException("Contact name is required.");
        }
        if (req.getContactPhone() == null || req.getContactPhone().isBlank()) {
            throw new BadRequestException("Contact phone is required.");
        }
        if (req.getPickupLocation() == null || req.getPickupLocation().isBlank()) {
            throw new BadRequestException("Pickup location is required.");
        }
        if (req.getDropLocation() == null || req.getDropLocation().isBlank()) {
            throw new BadRequestException("Drop location is required.");
        }
        if (req.getDistanceKm() == null || req.getDistanceKm() < 1) {
            throw new BadRequestException("Distance must be at least 1 km.");
        }
        if (req.getDistanceKm() > MAX_DISTANCE_KM) {
            throw new BadRequestException("Maximum distance for marriage bookings is " + MAX_DISTANCE_KM + " km.");
        }
        if (req.getWeddingDate() == null) {
            throw new BadRequestException("Wedding date is required.");
        }
        Month month = req.getWeddingDate().getMonth();
        if (!VALID_MONTHS.contains(month)) {
            throw new BadRequestException(
                    "Marriage bookings are available in March–June and October–December only.");
        }
    }

    private MarriageInquiryResponse toResponse(MarriageBookingInquiry entity) {
        MarriageInquiryResponse res = new MarriageInquiryResponse();
        res.setId(entity.getId());
        res.setContactName(entity.getContactName());
        res.setContactPhone(entity.getContactPhone());
        res.setPickupLocation(entity.getPickupLocation());
        res.setDropLocation(entity.getDropLocation());
        res.setDistanceKm(entity.getDistanceKm());
        res.setWeddingDate(entity.getWeddingDate());
        res.setEstimatedMinFare(entity.getEstimatedMinFare());
        res.setEstimatedMaxFare(entity.getEstimatedMaxFare());
        res.setMessage(entity.getMessage());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}
