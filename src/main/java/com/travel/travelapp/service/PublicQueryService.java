package com.travel.travelapp.service;

import com.travel.travelapp.dto.ScheduleSearchRequest;
import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.repository.BusRepository;
import com.travel.travelapp.repository.RouteRepository;
import com.travel.travelapp.repository.TripScheduleRepository;
import java.util.List;
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
        if (request.getDate() != null
                && request.getSource() != null
                && !request.getSource().isBlank()
                && request.getDestination() != null
                && !request.getDestination().isBlank()) {
            return tripScheduleRepository
                    .findByTravelDateAndRouteSourceIgnoreCaseAndRouteDestinationIgnoreCaseAndActiveTrue(
                            request.getDate(), request.getSource().trim(), request.getDestination().trim());
        }

        if (request.getDate() != null) {
            return tripScheduleRepository.findByTravelDateAndActiveTrue(request.getDate());
        }

        return tripScheduleRepository.findByActiveTrue();
    }
}
