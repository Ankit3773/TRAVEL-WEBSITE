package com.travel.travelapp.dto;

import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.PaymentGateway;
import com.travel.travelapp.entity.PaymentMode;
import com.travel.travelapp.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingHistoryResponse {
    private Long bookingId;
    private Long tripScheduleId;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String source;
    private String destination;
    private String busNumber;
    private List<Integer> seatNumbers;
    private Integer seatNumber;
    private String passengerName;
    private String passengerPhone;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private PaymentGateway paymentGateway;
    private String paymentReference;
    private LocalDateTime paidAt;
    private BigDecimal amount;
    private LocalDateTime bookedAt;
    private BookingStatus bookingStatus;
    private LocalDateTime cancelledAt;
    private Long cancelledByUserId;
    private String cancelledByName;
    private String cancelledByEmail;
    private Long bookedByUserId;
    private String bookedByName;
    private String bookedByEmail;
}
