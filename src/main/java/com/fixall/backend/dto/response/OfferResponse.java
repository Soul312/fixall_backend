package com.fixall.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OfferResponse {
    private String id;
    private String jobId;
    private String jobTitle;

    private String professionalId;
    private String professionalName;

    private BigDecimal amount;
    private String status;          // AWAITING_CLIENT, AWAITING_PRO, ACCEPTED, DECLINED

    /** Convenience flag: which party must act next ("CLIENT", "PRO", or null when closed). */
    private String awaitingParty;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
