package com.travel.travelapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingTrendPoint {
    private LocalDate date;
    private long confirmedBookings;
    private long cancelledBookings;
    private BigDecimal confirmedRevenue;
}
