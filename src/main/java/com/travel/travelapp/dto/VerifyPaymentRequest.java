package com.travel.travelapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {

    @NotBlank
    private String paymentSessionId;

    @NotBlank
    private String gatewayPaymentReference;
}
