package com.travel.travelapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateScheduleRequest {

    @NotNull
    private Long routeId;

    @NotNull
    private Long busId;

    @NotNull
    @FutureOrPresent
    private LocalDate travelDate;

    @NotNull
    private LocalTime departureTime;

    @NotNull
    private LocalTime arrivalTime;

    @DecimalMin("0.0")
    private BigDecimal baseFare;
}
