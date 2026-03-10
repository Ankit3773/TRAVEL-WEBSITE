package com.travel.travelapp.dto;

import com.travel.travelapp.entity.BookingStatus;
import com.travel.travelapp.entity.PaymentMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingResponse {
    private Long bookingId;
    private Long tripScheduleId;
    private List<Integer> seatNumbers;
    private Integer seatNumber;
    private String passengerName;
    private String passengerPhone;
    private PaymentMode paymentMode;
    private BigDecimal amount;
    private LocalDateTime bookedAt;
    private BookingStatus bookingStatus;
    private LocalDateTime lockExpiresAt;
}
