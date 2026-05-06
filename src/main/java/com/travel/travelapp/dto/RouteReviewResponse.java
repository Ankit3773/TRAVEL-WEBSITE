package com.travel.travelapp.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteReviewResponse {
    private Long reviewId;
    private Long routeId;
    private Long bookingId;
    private String source;
    private String destination;
    private String reviewerName;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
}
