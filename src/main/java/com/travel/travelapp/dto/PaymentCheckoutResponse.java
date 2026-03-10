package com.travel.travelapp.dto;

import com.travel.travelapp.entity.PaymentGateway;
import com.travel.travelapp.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCheckoutResponse {
    private Long bookingId;
    private PaymentGateway paymentGateway;
    private PaymentStatus paymentStatus;
    private String paymentSessionId;
    private BigDecimal amount;
    private LocalDateTime payableUntil;
}
