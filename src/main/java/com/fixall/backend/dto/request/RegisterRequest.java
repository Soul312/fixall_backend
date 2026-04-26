package com.fixall.backend.dto.request;

import com.fixall.backend.model.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    @NotNull
    private UserRole role;   // CLIENT or PROFESSIONAL
}
