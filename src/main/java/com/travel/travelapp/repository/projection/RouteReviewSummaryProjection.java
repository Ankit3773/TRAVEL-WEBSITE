package com.travel.travelapp.repository.projection;

public interface RouteReviewSummaryProjection {
    Long getRouteId();

    Double getAverageRating();

    Long getReviewCount();
}
