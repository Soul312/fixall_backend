package com.fixall.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    @NotBlank
    private String jobId;

    @NotBlank
    private String paymentIntentId;
}
