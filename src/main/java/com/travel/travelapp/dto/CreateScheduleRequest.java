package com.travel.travelapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScheduleRequest {

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

    @Size(max = 120)
    private String boardingPoint;

    @Size(max = 160)
    private String boardingNotes;

    @Size(max = 120)
    private String droppingPoint;

    @Size(max = 160)
    private String droppingNotes;

    @DecimalMin("0.0")
    private BigDecimal baseFare;
}
