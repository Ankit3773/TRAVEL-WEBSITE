package com.travel.travelapp.dto;

import com.travel.travelapp.entity.PaymentMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull
    private Long tripScheduleId;

    @Min(1)
    @Max(100)
    private Integer seatNumber;

    private List<@Min(1) @Max(100) Integer> seatNumbers;

    @NotBlank
    private String passengerName;

    @NotBlank
    private String passengerPhone;

    @NotNull
    private PaymentMode paymentMode;
}
