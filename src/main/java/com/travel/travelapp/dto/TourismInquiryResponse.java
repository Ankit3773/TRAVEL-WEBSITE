package com.travel.travelapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TourismInquiryResponse {

    private Long id;
    private String contactName;
    private String contactPhone;
    private String circuit;
    private Integer distanceKm;
    private LocalDate travelDate;
    private Integer groupSize;
    private BigDecimal estimatedFare;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
