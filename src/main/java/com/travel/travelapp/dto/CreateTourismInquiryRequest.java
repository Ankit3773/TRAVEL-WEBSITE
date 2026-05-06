package com.travel.travelapp.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTourismInquiryRequest {

    private String contactName;
    private String contactPhone;
    private String circuit;
    private Integer distanceKm;
    private LocalDate travelDate;
    private Integer groupSize;
    private String message;
}
