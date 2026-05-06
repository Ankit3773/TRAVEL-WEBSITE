package com.travel.travelapp.controller;

import com.travel.travelapp.dto.CreateRouteReviewRequest;
import com.travel.travelapp.dto.RouteReviewOverviewResponse;
import com.travel.travelapp.dto.RouteReviewResponse;
import com.travel.travelapp.dto.RouteReviewSummaryResponse;
import com.travel.travelapp.service.RouteReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class RouteReviewController {

    private final RouteReviewService routeReviewService;

    @GetMapping("/highlights")
    public List<RouteReviewResponse> highlights(@RequestParam(defaultValue = "6") int limit) {
        return routeReviewService.getHighlights(limit);
    }

    @GetMapping("/summary")
    public List<RouteReviewOverviewResponse> summaries(@RequestParam List<Long> routeIds) {
        return routeReviewService.getRouteReviewOverviews(routeIds);
    }

    @GetMapping("/route/{routeId}")
    public RouteReviewSummaryResponse routeSummary(
            @PathVariable Long routeId,
            @RequestParam(defaultValue = "4") int limit) {
        return routeReviewService.getRouteReviewSummary(routeId, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public RouteReviewResponse createOrUpdate(
            @Valid @RequestBody CreateRouteReviewRequest request,
            Authentication authentication) {
        return routeReviewService.createOrUpdateReview(request, authentication.getName());
    }
}
