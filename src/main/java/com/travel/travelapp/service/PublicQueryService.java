package com.travel.travelapp.service;

import com.travel.travelapp.dto.ScheduleSearchRequest;
import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.repository.BusRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicQueryService {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripScheduleRepository tripScheduleRepository;

    public List<Route> getActiveRoutes() {
        return routeRepository.findByActiveTrue();
    }

    public List<Route> getTourismRoutes() {
        return routeRepository.findByActiveTrueAndTourismRouteTrueOrderBySourceAscDestinationAsc();
    }

    public List<Bus> getActiveBuses() {
        return busRepository.findByActiveTrue();
    }

    public List<TripSchedule> searchSchedules(ScheduleSearchRequest request) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        LocalDate requestedDate = request.getDate();
        boolean hasSource = request.getSource() != null && !request.getSource().isBlank();
        boolean hasDestination = request.getDestination() != null && !request.getDestination().isBlank();

        // Reject any past date outright — booking sites don't sell tickets for the past.
        if (requestedDate != null && requestedDate.isBefore(today)) {
            return List.of();
        }

        List<TripSchedule> results;
        if (requestedDate != null && hasSource && hasDestination) {
            results = tripScheduleRepository
                    .findByTravelDateAndRouteSourceIgnoreCaseAndRouteDestinationIgnoreCaseAndActiveTrue(
                            requestedDate, request.getSource().trim(), request.getDestination().trim());
        } else if (requestedDate != null) {
            results = tripScheduleRepository.findByTravelDateAndActiveTrue(requestedDate);
        } else if (hasSource && hasDestination) {
            results = tripScheduleRepository
                    .findByActiveTrueAndTravelDateGreaterThanEqualAndRouteSourceIgnoreCaseAndRouteDestinationIgnoreCaseOrderByTravelDateAscDepartureTimeAsc(
                            today, request.getSource().trim(), request.getDestination().trim());
        } else {
            results = tripScheduleRepository
                    .findByActiveTrueAndTravelDateGreaterThanEqualOrderByTravelDateAscDepartureTimeAsc(today);
        }

        // Hide trips that have already departed today (it's 4 PM, no point selling the 7 AM bus).
        return results.stream()
                .filter(ts -> !ts.getTravelDate().isEqual(today) || !ts.getDepartureTime().isBefore(nowTime))
                .collect(Collectors.toList());
    }
}
