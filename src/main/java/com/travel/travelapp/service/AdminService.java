package com.travel.travelapp.service;

import com.travel.travelapp.dto.AdminMetricsResponse;
import com.travel.travelapp.dto.BookingTrendPoint;
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
import com.travel.travelapp.entity.Seat;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.exception.BadRequestException;
import com.travel.travelapp.exception.ResourceNotFoundException;
import com.travel.travelapp.repository.BusRepository;
import com.travel.travelapp.repository.BookingRepository;
import com.travel.travelapp.repository.BookingSeatRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.SeatRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import com.travel.travelapp.repository.projection.BookingDailyTrendProjection;
import com.travel.travelapp.util.FarePolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final List<BookingStatus> FINALIZED_BOOKING_STATUSES =
            List.of(BookingStatus.BOOKED, BookingStatus.CONFIRMED);

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripScheduleRepository tripScheduleRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public List<Route> getRoutes(boolean activeOnly) {
        return activeOnly
                ? routeRepository.findByActiveTrue()
                : routeRepository.findAllByOrderBySourceAscDestinationAsc();
    }

    public List<Bus> getBuses(boolean activeOnly) {
        return activeOnly
                ? busRepository.findByActiveTrue()
                : busRepository.findAllByOrderByBusNumberAsc();
    }

    public List<TripSchedule> getSchedules(boolean activeOnly) {
        return activeOnly
                ? tripScheduleRepository.findByActiveTrue()
                : tripScheduleRepository.findAllByOrderByTravelDateAscDepartureTimeAsc();
    }

    @Transactional
    public Route createRoute(CreateRouteRequest request) {
        String source = request.getSource().trim();
        String destination = request.getDestination().trim();

        Route route = routeRepository.findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .orElseGet(Route::new);

        route.setSource(source);
        route.setDestination(destination);
        route.setDistanceKm(request.getDistanceKm());
        route.setActive(true);
        route.setTourismRoute(false);
        Route savedRoute = routeRepository.save(route);
        refreshScheduleFaresForRoute(savedRoute);
        return savedRoute;
    }

    @Transactional
    public Route updateRoute(Long routeId, UpdateRouteRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        String source = request.getSource().trim();
        String destination = request.getDestination().trim();
        routeRepository.findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(routeId)) {
                        throw new BadRequestException("Another route already exists for source and destination");
                    }
                });

        route.setSource(source);
        route.setDestination(destination);
        route.setDistanceKm(request.getDistanceKm());
        route.setTourismRoute(request.getTourismRoute());
        route.setActive(true);
        Route savedRoute = routeRepository.save(route);
        refreshScheduleFaresForRoute(savedRoute);
        return savedRoute;
    }

    @Transactional
    public MessageResponse deactivateRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        route.setActive(false);
        routeRepository.save(route);

        for (TripSchedule schedule : tripScheduleRepository.findByRouteIdAndActiveTrue(routeId)) {
            schedule.setActive(false);
            tripScheduleRepository.save(schedule);
        }

        return MessageResponse.builder().message("Route deactivated successfully").build();
    }

    @Transactional
    public Bus createBus(CreateBusRequest request) {
        String busNumber = request.getBusNumber().trim().toUpperCase();
        if (busRepository.findByBusNumberIgnoreCase(busNumber).isPresent()) {
            throw new BadRequestException("Bus number already exists");
        }

        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setBusType(request.getBusType());
        bus.setTotalSeats(request.getTotalSeats());
        bus.setActive(true);
        Bus savedBus = busRepository.save(bus);
        syncSeatsForBus(savedBus);
        return savedBus;
    }

    @Transactional
    public Bus updateBus(Long busId, UpdateBusRequest request) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        String busNumber = request.getBusNumber().trim().toUpperCase();
        busRepository.findByBusNumberIgnoreCase(busNumber)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(busId)) {
                        throw new BadRequestException("Another bus already uses this bus number");
                    }
                });

        bus.setBusNumber(busNumber);
        bus.setBusType(request.getBusType());
        bus.setTotalSeats(request.getTotalSeats());
        bus.setActive(true);
        Bus savedBus = busRepository.save(bus);
        syncSeatsForBus(savedBus);
        return savedBus;
    }

    @Transactional
    public MessageResponse deactivateBus(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        bus.setActive(false);
        busRepository.save(bus);

        for (TripSchedule schedule : tripScheduleRepository.findByBusIdAndActiveTrue(busId)) {
            schedule.setActive(false);
            tripScheduleRepository.save(schedule);
        }

        for (Seat seat : seatRepository.findByBusIdOrderBySeatNumberAsc(busId)) {
            if (Boolean.TRUE.equals(seat.getActive())) {
                seat.setActive(false);
                seatRepository.save(seat);
            }
        }

        return MessageResponse.builder().message("Bus deactivated successfully").build();
    }

    @Transactional
    public TripSchedule createSchedule(CreateScheduleRequest request) {
        validateScheduleTiming(request.getDepartureTime(), request.getArrivalTime());
        Route route = requireActiveRoute(request.getRouteId());
        Bus bus = requireActiveBus(request.getBusId());

        if (tripScheduleRepository.existsByBusIdAndTravelDateAndRouteIdAndDepartureTime(
                bus.getId(), request.getTravelDate(), route.getId(), request.getDepartureTime())) {
            throw new BadRequestException("A schedule already exists for this bus, route, date and departure time");
        }

        TripSchedule schedule = new TripSchedule();
        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setTravelDate(request.getTravelDate());
        schedule.setDepartureTime(request.getDepartureTime());
        schedule.setArrivalTime(request.getArrivalTime());
        schedule.setBaseFare(FarePolicy.fareFor(route, bus));
        schedule.setActive(true);

        return tripScheduleRepository.save(schedule);
    }

    @Transactional
    public TripSchedule updateSchedule(Long scheduleId, UpdateScheduleRequest request) {
        validateScheduleTiming(request.getDepartureTime(), request.getArrivalTime());

        TripSchedule schedule = tripScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        Route route = requireActiveRoute(request.getRouteId());
        Bus bus = requireActiveBus(request.getBusId());

        boolean scheduleIdentityChanged = !Objects.equals(schedule.getBus().getId(), bus.getId())
                || !Objects.equals(schedule.getRoute().getId(), route.getId())
                || !Objects.equals(schedule.getTravelDate(), request.getTravelDate())
                || !Objects.equals(schedule.getDepartureTime(), request.getDepartureTime());

        if (scheduleIdentityChanged && tripScheduleRepository.existsByBusIdAndTravelDateAndRouteIdAndDepartureTime(
                bus.getId(), request.getTravelDate(), route.getId(), request.getDepartureTime())) {
            throw new BadRequestException("A schedule already exists for this bus, route, date and departure time");
        }

        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setTravelDate(request.getTravelDate());
        schedule.setDepartureTime(request.getDepartureTime());
        schedule.setArrivalTime(request.getArrivalTime());
        schedule.setBaseFare(FarePolicy.fareFor(route, bus));
        schedule.setActive(true);

        return tripScheduleRepository.save(schedule);
    }

    @Transactional
    public MessageResponse deactivateSchedule(Long scheduleId) {
        TripSchedule schedule = tripScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        schedule.setActive(false);
        tripScheduleRepository.save(schedule);
        return MessageResponse.builder().message("Schedule deactivated successfully").build();
    }

    @Transactional
    public MessageResponse generateSeatsForAllActiveBuses() {
        long busCount = 0;
        long totalActiveSeats = 0;
        for (Bus bus : busRepository.findByActiveTrue()) {
            busCount++;
            totalActiveSeats += syncSeatsForBus(bus);
        }
        return MessageResponse.builder()
                .message("Seat inventory synced for " + busCount + " buses. Active seats: " + totalActiveSeats)
                .build();
    }

    public AdminMetricsResponse getMetrics() {
        long totalBookings = bookingRepository.countByBookingStatusNot(BookingStatus.LOCKED);
        long confirmedBookings = bookingRepository.countByBookingStatusIn(FINALIZED_BOOKING_STATUSES);
        long cancelledBookings = bookingRepository.countByBookingStatus(BookingStatus.CANCELLED);
        long activeRoutes = routeRepository.countByActiveTrue();
        long activeBuses = busRepository.countByActiveTrue();
        long activeSchedules = tripScheduleRepository.countByActiveTrue();
        long upcomingSchedules = tripScheduleRepository.countByTravelDateGreaterThanEqualAndActiveTrue(LocalDate.now());
        long totalSeatCapacity = tripScheduleRepository.sumSeatCapacityForActiveSchedules();
        long confirmedSeats = bookingSeatRepository.countByBookingBookingStatusInAndActiveTrue(FINALIZED_BOOKING_STATUSES);

        double occupancyPercent = totalSeatCapacity == 0
                ? 0.0
                : (confirmedSeats * 100.0) / totalSeatCapacity;

        return AdminMetricsResponse.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .activeRoutes(activeRoutes)
                .activeBuses(activeBuses)
                .activeSchedules(activeSchedules)
                .upcomingSchedules(upcomingSchedules)
                .totalSeatCapacity(totalSeatCapacity)
                .occupancyPercent(Math.round(occupancyPercent * 100.0) / 100.0)
                .build();
    }

    public BookingTrendResponse getBookingTrends(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveToDate = toDate == null ? LocalDate.now() : toDate;
        LocalDate effectiveFromDate = fromDate == null ? effectiveToDate.minusDays(6) : fromDate;

        if (effectiveFromDate.isAfter(effectiveToDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime fromDateTime = effectiveFromDate.atStartOfDay();
        LocalDateTime toExclusive = effectiveToDate.plusDays(1).atStartOfDay();
        List<BookingDailyTrendProjection> rawTrend = bookingRepository.findDailyBookingTrend(
                fromDateTime, toExclusive, List.of(BookingStatus.BOOKED, BookingStatus.CONFIRMED, BookingStatus.CANCELLED));

        Map<LocalDate, DailyTrendAccumulator> accumulatorByDate = new LinkedHashMap<>();
        LocalDate cursor = effectiveFromDate;
        while (!cursor.isAfter(effectiveToDate)) {
            accumulatorByDate.put(cursor, new DailyTrendAccumulator(cursor));
            cursor = cursor.plusDays(1);
        }

        for (BookingDailyTrendProjection row : rawTrend) {
            DailyTrendAccumulator accumulator = accumulatorByDate.get(row.getBookingDate());
            if (accumulator == null) {
                continue;
            }

            if (row.getBookingStatus().isBookedState()) {
                accumulator.confirmedBookings += row.getBookingCount();
                accumulator.confirmedRevenue = accumulator.confirmedRevenue.add(
                        row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount());
            } else if (row.getBookingStatus() == BookingStatus.CANCELLED) {
                accumulator.cancelledBookings += row.getBookingCount();
            }
        }

        List<BookingTrendPoint> items = new ArrayList<>();
        long totalConfirmed = 0;
        long totalCancelled = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (DailyTrendAccumulator accumulator : accumulatorByDate.values()) {
            items.add(BookingTrendPoint.builder()
                    .date(accumulator.date)
                    .confirmedBookings(accumulator.confirmedBookings)
                    .cancelledBookings(accumulator.cancelledBookings)
                    .confirmedRevenue(accumulator.confirmedRevenue)
                    .build());

            totalConfirmed += accumulator.confirmedBookings;
            totalCancelled += accumulator.cancelledBookings;
            totalRevenue = totalRevenue.add(accumulator.confirmedRevenue);
        }

        return BookingTrendResponse.builder()
                .fromDate(effectiveFromDate)
                .toDate(effectiveToDate)
                .totalConfirmedBookings(totalConfirmed)
                .totalCancelledBookings(totalCancelled)
                .totalConfirmedRevenue(totalRevenue)
                .items(items)
                .build();
    }

    private void validateScheduleTiming(java.time.LocalTime departureTime, java.time.LocalTime arrivalTime) {
        if (!arrivalTime.isAfter(departureTime)) {
            throw new BadRequestException("Arrival time must be after departure time");
        }
    }

    private void refreshScheduleFaresForRoute(Route route) {
        for (TripSchedule schedule : tripScheduleRepository.findByRouteIdAndActiveTrue(route.getId())) {
            schedule.setBaseFare(FarePolicy.fareFor(route, schedule.getBus()));
            tripScheduleRepository.save(schedule);
        }
    }

    private Route requireActiveRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        if (!Boolean.TRUE.equals(route.getActive())) {
            throw new BadRequestException("Route is inactive");
        }
        return route;
    }

    private Bus requireActiveBus(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
        if (!Boolean.TRUE.equals(bus.getActive())) {
            throw new BadRequestException("Bus is inactive");
        }
        return bus;
    }

    private long syncSeatsForBus(Bus bus) {
        List<Seat> currentSeats = seatRepository.findByBusIdOrderBySeatNumberAsc(bus.getId());
        Map<Integer, Seat> bySeatNumber = new HashMap<>();
        for (Seat seat : currentSeats) {
            bySeatNumber.put(seat.getSeatNumber(), seat);
        }

        for (int seatNumber = 1; seatNumber <= bus.getTotalSeats(); seatNumber++) {
            Seat seat = bySeatNumber.get(seatNumber);
            if (seat == null) {
                Seat newSeat = new Seat();
                newSeat.setBus(bus);
                newSeat.setSeatNumber(seatNumber);
                newSeat.setActive(true);
                seatRepository.save(newSeat);
            } else if (!Boolean.TRUE.equals(seat.getActive())) {
                seat.setActive(true);
                seatRepository.save(seat);
            }
        }

        for (Seat seat : currentSeats) {
            if (seat.getSeatNumber() > bus.getTotalSeats() && Boolean.TRUE.equals(seat.getActive())) {
                seat.setActive(false);
                seatRepository.save(seat);
            }
        }

        return seatRepository.countByBusIdAndActiveTrue(bus.getId());
    }

    private static class DailyTrendAccumulator {
        private final LocalDate date;
        private long confirmedBookings;
        private long cancelledBookings;
        private BigDecimal confirmedRevenue = BigDecimal.ZERO;

        private DailyTrendAccumulator(LocalDate date) {
            this.date = date;
        }
    }
}
