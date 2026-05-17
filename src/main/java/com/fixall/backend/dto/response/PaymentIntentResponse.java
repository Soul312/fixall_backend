package com.fixall.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private String paymentIntentId;
    private String jobId;
    private Long amountCents;
    private String currency;
}
