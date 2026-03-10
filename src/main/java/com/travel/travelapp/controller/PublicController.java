package com.travel.travelapp.controller;

import com.travel.travelapp.dto.ScheduleSearchRequest;
import com.travel.travelapp.entity.Bus;
import com.travel.travelapp.entity.Route;
import com.travel.travelapp.entity.TripSchedule;
import com.travel.travelapp.service.PublicQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Public")
public class PublicController {

    private final PublicQueryService publicQueryService;

    @GetMapping("/routes")
    public List<Route> getRoutes() {
        return publicQueryService.getActiveRoutes();
    }

    @GetMapping("/routes/tourism")
    public List<Route> getTourismRoutes() {
        return publicQueryService.getTourismRoutes();
    }

    @GetMapping("/buses")
    public List<Bus> getBuses() {
        return publicQueryService.getActiveBuses();
    }

    @GetMapping("/schedules")
    public List<TripSchedule> getSchedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination) {
        ScheduleSearchRequest request = new ScheduleSearchRequest();
        request.setDate(date);
        request.setSource(source);
        request.setDestination(destination);
        return publicQueryService.searchSchedules(request);
    }
}
