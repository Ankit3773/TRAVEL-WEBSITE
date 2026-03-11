package com.travel.travelapp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {

    @NotBlank
    private String paymentSessionId;

    @NotBlank
    @JsonAlias("gatewayPaymentReference")
    private String paymentId;
}
