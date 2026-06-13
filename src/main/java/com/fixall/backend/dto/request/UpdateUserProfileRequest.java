package com.fixall.backend.dto.request;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String fullName;
    private String phone;
}
