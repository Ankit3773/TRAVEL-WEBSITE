package com.travel.travelapp.controller;

import com.travel.travelapp.dto.AdminMetricsResponse;
import com.travel.travelapp.dto.BookingHistoryResponse;
import com.travel.travelapp.dto.BookingPageResponse;
import com.travel.travelapp.dto.BookingTrendResponse;
import com.travel.travelapp.dto.CreateBusRequest;
import com.travel.travelapp.dto.CreateRouteRequest;
import com.travel.travelapp.dto.CreateScheduleRequest;
import com.travel.travelapp.dto.MessageResponse;
import com.travel.travelapp.dto.UpdateBusRequest;
import com.travel.travelapp.dto.UpdateRouteRequest;
import com.travel.travelapp.dto.UpdateScheduleRequest;
import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.service.AdminService;
import com.travel.travelapp.service.BookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;

    @GetMapping("/routes")
    public List<Route> getRoutes(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return adminService.getRoutes(activeOnly);
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public Route createRoute(@Valid @RequestBody CreateRouteRequest request) {
        return adminService.createRoute(request);
    }

    @PutMapping("/routes/{routeId}")
    public Route updateRoute(@PathVariable Long routeId, @Valid @RequestBody UpdateRouteRequest request) {
        return adminService.updateRoute(routeId, request);
    }

    @DeleteMapping("/routes/{routeId}")
    public MessageResponse deleteRoute(@PathVariable Long routeId) {
        return adminService.deactivateRoute(routeId);
    }

    @GetMapping("/buses")
    public List<Bus> getBuses(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return adminService.getBuses(activeOnly);
    }

    @PostMapping("/buses")
    @ResponseStatus(HttpStatus.CREATED)
    public Bus createBus(@Valid @RequestBody CreateBusRequest request) {
        return adminService.createBus(request);
    }

    @PutMapping("/buses/{busId}")
    public Bus updateBus(@PathVariable Long busId, @Valid @RequestBody UpdateBusRequest request) {
        return adminService.updateBus(busId, request);
    }

    @DeleteMapping("/buses/{busId}")
    public MessageResponse deleteBus(@PathVariable Long busId) {
        return adminService.deactivateBus(busId);
    }

    @PostMapping("/seats/generate")
    public MessageResponse generateSeatsForAllBuses() {
        return adminService.generateSeatsForAllActiveBuses();
    }

    @GetMapping("/schedules")
    public List<TripSchedule> getSchedules(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return adminService.getSchedules(activeOnly);
    }

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public TripSchedule createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return adminService.createSchedule(request);
    }

    @PutMapping("/schedules/{scheduleId}")
    public TripSchedule updateSchedule(@PathVariable Long scheduleId, @Valid @RequestBody UpdateScheduleRequest request) {
        return adminService.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public MessageResponse deleteSchedule(@PathVariable Long scheduleId) {
        return adminService.deactivateSchedule(scheduleId);
    }

    @GetMapping("/bookings")
    public List<BookingHistoryResponse> getAllBookings() {
        return bookingService.getAllBookingsForAdmin();
    }

    @GetMapping("/bookings/paged")
    public BookingPageResponse getAllBookingsPaged(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bookingService.getAllBookingsForAdminPaged(status, page, size);
    }

    @GetMapping("/metrics")
    public AdminMetricsResponse getMetrics() {
        return adminService.getMetrics();
    }

    @GetMapping("/metrics/trends")
    public BookingTrendResponse getBookingTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return adminService.getBookingTrends(fromDate, toDate);
    }
}
