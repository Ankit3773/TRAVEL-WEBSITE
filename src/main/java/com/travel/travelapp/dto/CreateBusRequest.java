package com.travel.travelapp.dto;

import com.travel.travelapp.entity.BusType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBusRequest {

    @NotBlank
    private String busNumber;

    @NotNull
    private BusType busType;

    @NotNull
    @Min(1)
    private Integer totalSeats;
}
