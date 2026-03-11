package com.travel.travelapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRouteRequest {

    @NotBlank
    private String source;

    @NotBlank
    private String destination;

    @NotNull
    @Min(1)
    private Integer distanceKm;

    private Boolean tourismRoute;
}
