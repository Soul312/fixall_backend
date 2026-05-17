package com.fixall.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class UpdateProfessionalProfileRequest {
    private String bio;
    private List<String> categories;
}

