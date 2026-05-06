package com.travel.travelapp.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteReviewSummaryResponse {
    private Long routeId;
    private Double averageRating;
    private Long totalReviews;
    private List<RouteReviewResponse> reviews;
}
