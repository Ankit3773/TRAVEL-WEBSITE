package com.travel.travelapp.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteReviewOverviewResponse {
    private Long routeId;
    private Double averageRating;
    private Long totalReviews;
}
