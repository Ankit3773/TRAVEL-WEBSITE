package com.travel.travelapp.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatAvailabilityResponse {
    private Long tripScheduleId;
    private Integer totalSeats;
    private List<Integer> bookedSeats;
    private List<Integer> lockedSeats;
    private List<Integer> availableSeats;
}
