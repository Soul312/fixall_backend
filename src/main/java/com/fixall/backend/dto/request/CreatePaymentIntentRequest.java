package com.fixall.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreatePaymentIntentRequest {
    @NotBlank
    private String jobId;

    @NotNull
    @Positive
    private Long amountCents;   // Amount in centimes (e.g. 5000 = 50.00 MAD/EUR)
}
