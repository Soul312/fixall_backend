package com.fixall.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRatingRequest {
    @NotBlank
    private String jobId;

    @NotBlank
    private String professionalId;

    @NotNull
    @Min(1) @Max(5)
    private Integer score;

    private String comment;
}
