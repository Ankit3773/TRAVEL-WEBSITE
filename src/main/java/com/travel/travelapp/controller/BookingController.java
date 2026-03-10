package com.travel.travelapp.controller;

import com.travel.travelapp.dto.BookingHistoryResponse;
import com.travel.travelapp.dto.BookingPageResponse;
import com.travel.travelapp.dto.BookingResponse;
import com.travel.travelapp.dto.CreateBookingRequest;
import com.travel.travelapp.dto.PaymentCheckoutResponse;
import com.travel.travelapp.dto.SeatAvailabilityResponse;
import com.travel.travelapp.dto.VerifyPaymentRequest;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.service.BookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Booking")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/schedules/{scheduleId}/seats")
    public SeatAvailabilityResponse getSeatAvailability(@PathVariable Long scheduleId) {
        return bookingService.getSeatAvailability(scheduleId);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.createBooking(request, authentication.getName());
    }

    @PostMapping("/bookings/locks")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public BookingResponse lockSeats(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.createSeatLock(request, authentication.getName());
    }

    @PostMapping("/bookings/locks/{bookingId}/confirm")
    @SecurityRequirement(name = "bearerAuth")
    public BookingResponse confirmLock(@PathVariable Long bookingId, Authentication authentication) {
        return bookingService.confirmSeatLock(bookingId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/bookings/locks/{bookingId}/payments/checkout")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentCheckoutResponse startPaymentCheckout(@PathVariable Long bookingId, Authentication authentication) {
        return bookingService.createPaymentCheckout(bookingId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/bookings/locks/{bookingId}/payments/verify")
    @SecurityRequirement(name = "bearerAuth")
    public BookingResponse verifyPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {
        return bookingService.verifyPayment(bookingId, request, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/my-bookings")
    @SecurityRequirement(name = "bearerAuth")
    public List<BookingHistoryResponse> myBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }

    @GetMapping("/my-bookings/paged")
    @SecurityRequirement(name = "bearerAuth")
    public BookingPageResponse myBookingsPaged(
            Authentication authentication,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bookingService.getMyBookingsPaged(authentication.getName(), status, page, size);
    }

    @GetMapping("/bookings/{bookingId}")
    @SecurityRequirement(name = "bearerAuth")
    public BookingResponse getBooking(@PathVariable Long bookingId, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return bookingService.getBookingById(bookingId, authentication.getName(), isAdmin);
    }

    @DeleteMapping("/bookings/locks/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void releaseBookingLock(@PathVariable Long bookingId, Authentication authentication) {
        bookingService.releaseSeatLock(bookingId, authentication.getName(), isAdmin(authentication));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void cancelBooking(@PathVariable Long bookingId, Authentication authentication) {
        bookingService.cancelBooking(bookingId, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
