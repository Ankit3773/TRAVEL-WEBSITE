package com.travel.travelapp.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForgotPasswordResponse {
    private String message;
    private String resetToken;
}
