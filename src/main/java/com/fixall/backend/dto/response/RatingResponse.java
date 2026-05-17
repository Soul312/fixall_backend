package com.fixall.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private String id;
    private String jobId;
    private String professionalId;
    private String professionalName;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
