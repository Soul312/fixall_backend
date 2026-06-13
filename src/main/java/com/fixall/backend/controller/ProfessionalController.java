package com.fixall.backend.controller;

import com.fixall.backend.dto.request.UpdateProfessionalProfileRequest;
import com.fixall.backend.model.ProfessionalProfile;
import com.fixall.backend.model.User;
import com.fixall.backend.service.ProfessionalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pro")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalProfileService profileService;

    @GetMapping("/profile")
    public ResponseEntity<ProfessionalProfile> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getByUserId(user.getId()));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfessionalProfile> saveProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfessionalProfileRequest req) {
        ProfessionalProfile saved = profileService.saveOrUpdate(user.getId(), req);
        return ResponseEntity.ok(saved);
    }
}

