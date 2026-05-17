package com.fixall.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class JobResponse {
    private String id;
    private String title;
    private String description;
    private String category;
    private String status;

    private String clientId;
    private String clientName;

    private String professionalId;
    private String professionalName;

    private Double latitude;
    private Double longitude;

    private BigDecimal estimatedPrice;
    private BigDecimal actualPrice;

    private String paymentStatus;
    private String stripePaymentIntentId;

    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
