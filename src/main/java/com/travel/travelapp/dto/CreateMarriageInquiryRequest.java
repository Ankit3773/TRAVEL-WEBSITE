package com.travel.travelapp.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMarriageInquiryRequest {

    private String contactName;
    private String contactPhone;
    private String pickupLocation;
    private String dropLocation;
    private Integer distanceKm;
    private LocalDate weddingDate;
    private String message;
}
