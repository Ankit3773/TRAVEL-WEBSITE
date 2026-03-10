package com.travel.travelapp.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminMetricsResponse {
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long activeRoutes;
    private long activeBuses;
    private long activeSchedules;
    private long upcomingSchedules;
    private long totalSeatCapacity;
    private double occupancyPercent;
}
