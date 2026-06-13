package com.fixall.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateProfessionalProfileRequest {
    private String businessName;
    private String bio;
    private List<String> categories;
}
