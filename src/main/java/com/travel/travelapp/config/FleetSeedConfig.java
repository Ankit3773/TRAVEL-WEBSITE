package com.travel.travelapp.config;

import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.BusType;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.repository.BusRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import com.travel.travelapp.util.FarePolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FleetSeedConfig {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripScheduleRepository tripScheduleRepository;

    private int acBusSeq = 1;
    private int nonAcBusSeq = 1;
    private int tourismAcSeq = 1;
    private final Set<String> plannedBusNumbers = new HashSet<>();

    @Bean
    public CommandLineRunner seedFleetData(
            @Value("${app.seed.fleet.enabled:true}") boolean seedEnabled,
            @Value("${app.seed.fleet.prune-legacy:true}") boolean pruneLegacy) {
        return args -> {
            if (!seedEnabled) {
                return;
            }

            plannedBusNumbers.clear();
            try {
                // Daily commute fleet (15 buses total)
                seedRoutePair("Gaya", 110, 3, 1, LocalTime.of(8, 0), 180, false);
                seedRoutePair("Jehanabad", 50, 2, 1, LocalTime.of(7, 30), 105, false);
                seedRoutePair("Chapra", 75, 2, 1, LocalTime.of(9, 30), 150, false);
                seedRoutePair("Arrah", 65, 2, 1, LocalTime.of(8, 30), 105, false);
                seedRoutePair("Bihta", 35, 1, 1, LocalTime.of(9, 0), 60, false);

                // Tourism fleet (5 AC buses total, AC only) - Bihar key tourism circuits
                seedRoutePair("Bodh Gaya", 125, 1, 0, LocalTime.of(6, 30), 210, true);
                seedRoutePair("Rajgir", 100, 1, 0, LocalTime.of(6, 0), 150, true);
                seedRoutePair("Nalanda", 95, 1, 0, LocalTime.of(7, 0), 165, true);
                seedRoutePair("Vaishali", 60, 1, 0, LocalTime.of(8, 0), 120, true);
                seedRoutePair("Pawapuri", 95, 1, 0, LocalTime.of(7, 30), 165, true);

                if (pruneLegacy) {
                    deactivateLegacyFleet();
                }

                syncActiveScheduleFares();
            } catch (Exception ex) {
                log.warn("Fleet seed sync skipped due to startup database error: {}", ex.getMessage(), ex);
            }
        };
    }

    private void seedRoutePair(
            String city,
            int distanceKm,
            int acCount,
            int nonAcCount,
            LocalTime outboundDeparture,
            int travelMinutes,
            boolean tourismBusNumbering) {
        Route outbound = getOrCreateRoute("Patna", city, distanceKm, tourismBusNumbering);
        Route inbound = getOrCreateRoute(city, "Patna", distanceKm, tourismBusNumbering);
        int totalBusCount = acCount + nonAcCount;
        int departureSpacingMinutes = departureSpacingMinutes(totalBusCount);
        int busIndex = 0;

        for (int i = 0; i < acCount; i++) {
            String busNumber = tourismBusNumbering
                    ? String.format("NT-TO-AC-%03d", tourismAcSeq++)
                    : String.format("NT-AC-%03d", acBusSeq++);
            Bus bus = getOrCreateBus(busNumber, BusType.AC, tourismBusNumbering ? 36 : 40);
            BigDecimal fare = FarePolicy.fareFor(outbound, bus);
            seedBidirectionalSchedules(
                    bus,
                    outbound,
                    inbound,
                    outboundDeparture,
                    departureSpacingMinutes,
                    busIndex++,
                    travelMinutes,
                    fare);
        }

        for (int i = 0; i < nonAcCount; i++) {
            String busNumber = String.format("NT-NA-%03d", nonAcBusSeq++);
            Bus bus = getOrCreateBus(busNumber, BusType.NON_AC, 42);
            BigDecimal fare = FarePolicy.fareFor(outbound, bus);
            seedBidirectionalSchedules(
                    bus,
                    outbound,
                    inbound,
                    outboundDeparture,
                    departureSpacingMinutes,
                    busIndex++,
                    travelMinutes,
                    fare);
        }
    }

    static int departureSpacingMinutes(int totalBusCount) {
        if (totalBusCount <= 1) {
            return 0;
        }
        if (totalBusCount <= 3) {
            return 180;
        }
        if (totalBusCount <= 5) {
            return 90;
        }
        return 60;
    }

    private void seedBidirectionalSchedules(
            Bus bus,
            Route outbound,
            Route inbound,
            LocalTime firstOutboundDeparture,
            int departureSpacingMinutes,
            int busIndex,
            int travelMinutes,
            BigDecimal fare) {
        for (int day = 1; day <= 3; day++) {
            LocalDate travelDate = LocalDate.now().plusDays(day);
            LocalTime outboundDeparture = firstOutboundDeparture.plusMinutes((long) departureSpacingMinutes * busIndex);
            LocalTime outboundArrival = outboundDeparture.plusMinutes(travelMinutes);
            LocalTime firstInboundDeparture = firstOutboundDeparture.plusMinutes(travelMinutes).plusHours(2);
            LocalTime inboundDeparture = firstInboundDeparture.plusMinutes((long) departureSpacingMinutes * busIndex);
            LocalTime inboundArrival = inboundDeparture.plusMinutes(travelMinutes);

            createScheduleIfMissing(bus, outbound, travelDate, outboundDeparture, outboundArrival, fare);
            createScheduleIfMissing(bus, inbound, travelDate, inboundDeparture, inboundArrival, fare);
        }
    }

    private Route getOrCreateRoute(String source, String destination, int distanceKm, boolean tourismRoute) {
        return routeRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .map(route -> {
                    route.setDistanceKm(distanceKm);
                    route.setActive(true);
                    route.setTourismRoute(tourismRoute);
                    return routeRepository.save(route);
                })
                .orElseGet(() -> {
                    Route route = new Route();
                    route.setSource(source);
                    route.setDestination(destination);
                    route.setDistanceKm(distanceKm);
                    route.setActive(true);
                    route.setTourismRoute(tourismRoute);
                    return routeRepository.save(route);
                });
    }

    private Bus getOrCreateBus(String busNumber, BusType busType, int totalSeats) {
        plannedBusNumbers.add(busNumber.toUpperCase());
        return busRepository
                .findByBusNumberIgnoreCase(busNumber)
                .map(bus -> {
                    bus.setBusType(busType);
                    bus.setTotalSeats(totalSeats);
                    bus.setActive(true);
                    return busRepository.save(bus);
                })
                .orElseGet(() -> {
                    Bus bus = new Bus();
                    bus.setBusNumber(busNumber);
                    bus.setBusType(busType);
                    bus.setTotalSeats(totalSeats);
                    bus.setActive(true);
                    return busRepository.save(bus);
                });
    }

    private void createScheduleIfMissing(
            Bus bus,
            Route route,
            LocalDate travelDate,
            LocalTime departureTime,
            LocalTime arrivalTime,
            BigDecimal fare) {
        var matchingSchedules = tripScheduleRepository.findAllByBusIdAndTravelDateAndRouteIdOrderByDepartureTimeAsc(
                bus.getId(), travelDate, route.getId());
        if (!matchingSchedules.isEmpty()) {
            TripSchedule schedule = matchingSchedules.get(0);
            schedule.setDepartureTime(departureTime);
            schedule.setArrivalTime(arrivalTime);
            schedule.setBaseFare(fare);
            schedule.setActive(true);
            tripScheduleRepository.save(schedule);

            for (int i = 1; i < matchingSchedules.size(); i++) {
                TripSchedule duplicate = matchingSchedules.get(i);
                duplicate.setActive(false);
                tripScheduleRepository.save(duplicate);
            }
            return;
        }

        TripSchedule schedule = new TripSchedule();
        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setTravelDate(travelDate);
        schedule.setDepartureTime(departureTime);
        schedule.setArrivalTime(arrivalTime);
        schedule.setBaseFare(fare);
        schedule.setActive(true);
        tripScheduleRepository.save(schedule);
    }

    private void deactivateLegacyFleet() {
        for (Bus bus : busRepository.findAll()) {
            String normalized = bus.getBusNumber() == null ? "" : bus.getBusNumber().toUpperCase();
            if (!plannedBusNumbers.contains(normalized) && Boolean.TRUE.equals(bus.getActive())) {
                bus.setActive(false);
                busRepository.save(bus);
                deactivateSchedulesForBus(bus.getId());
            }
        }
    }

    private void deactivateSchedulesForBus(Long busId) {
        for (TripSchedule schedule : tripScheduleRepository.findByBusIdAndActiveTrue(busId)) {
            schedule.setActive(false);
            tripScheduleRepository.save(schedule);
        }
    }

    private void syncActiveScheduleFares() {
        for (TripSchedule schedule : tripScheduleRepository.findByActiveTrue()) {
            schedule.setBaseFare(FarePolicy.fareFor(schedule.getRoute(), schedule.getBus()));
            tripScheduleRepository.save(schedule);
        }
    }
}
