package com.travel.travelapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "marriage_booking_inquiries")
public class MarriageBookingInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(nullable = false, length = 120)
    private String pickupLocation;

    @Column(nullable = false, length = 120)
    private String dropLocation;

    @Column(nullable = false)
    private Integer distanceKm;

    @Column(nullable = false)
    private LocalDate weddingDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedMinFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedMaxFare;

    @Column(length = 400)
    private String message;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
