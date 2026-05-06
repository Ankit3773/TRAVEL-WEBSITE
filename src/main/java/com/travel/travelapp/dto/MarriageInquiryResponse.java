package com.travel.travelapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarriageInquiryResponse {

    private Long id;
    private String contactName;
    private String contactPhone;
    private String pickupLocation;
    private String dropLocation;
    private Integer distanceKm;
    private LocalDate weddingDate;
    private BigDecimal estimatedMinFare;
    private BigDecimal estimatedMaxFare;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
