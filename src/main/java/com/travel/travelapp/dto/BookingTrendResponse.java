package com.travel.travelapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingTrendResponse {
    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalConfirmedBookings;
    private long totalCancelledBookings;
    private BigDecimal totalConfirmedRevenue;
    private List<BookingTrendPoint> items;
}
