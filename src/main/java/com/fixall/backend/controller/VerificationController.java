package com.fixall.backend.controller;

import com.fixall.backend.exception.BadRequestException;
import com.fixall.backend.exception.ForbiddenException;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.ProfessionalProfile;
import com.fixall.backend.model.User;
import com.fixall.backend.model.enums.UserRole;
import com.fixall.backend.model.enums.VerificationStatus;
import com.fixall.backend.repository.ProfessionalProfileRepository;
import com.fixall.backend.repository.UserRepository;
import com.fixall.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final FileStorageService fileStorageService;
    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    /**
     * POST /api/verification/upload-id
     * Professional uploads their ID document for verification.
     * Sets verification status to PENDING.
     */
    @PostMapping("/upload-id")
    public ResponseEntity<Map<String, String>> uploadIdDocument(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new ForbiddenException("Only professionals can upload ID documents");
        }

        ProfessionalProfile profile = profileRepository.findByUser_Id(user.getId())
            .orElseThrow(() -> new BadRequestException(
                "Professional profile must be created before uploading ID. Call POST /api/pro/profile first."));

        // Store the file
        String fileUrl = fileStorageService.storeFile(file, "id-docs");

        // Delete old document if one exists
        if (profile.getIdDocumentUrl() != null) {
            fileStorageService.deleteFile(profile.getIdDocumentUrl());
        }

        // Update profile
        profile.setIdDocumentUrl(fileUrl);
        profileRepository.save(profile);

        // Set user verification to PENDING (admin will review)
        user.setVerificationStatus(VerificationStatus.PENDING);
        userRepository.save(user);

        log.info("ID document uploaded by user {} -> {}", user.getId(), fileUrl);

        return ResponseEntity.ok(Map.of(
            "message", "ID document uploaded successfully. Verification is pending admin review.",
            "fileUrl", fileUrl
        ));
    }

    /**
     * POST /api/verification/upload-certification
     * Professional uploads optional certification documents.
     */
    @PostMapping("/upload-certification")
    public ResponseEntity<Map<String, String>> uploadCertification(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new ForbiddenException("Only professionals can upload certifications");
        }

        ProfessionalProfile profile = profileRepository.findByUser_Id(user.getId())
            .orElseThrow(() -> new BadRequestException(
                "Professional profile must be created before uploading certifications."));

        String fileUrl = fileStorageService.storeFile(file, "certifications");

        if (profile.getCertificationUrl() != null) {
            fileStorageService.deleteFile(profile.getCertificationUrl());
        }

        profile.setCertificationUrl(fileUrl);
        profileRepository.save(profile);

        log.info("Certification uploaded by user {} -> {}", user.getId(), fileUrl);

        return ResponseEntity.ok(Map.of(
            "message", "Certification uploaded successfully.",
            "fileUrl", fileUrl
        ));
    }

    /**
     * GET /api/verification/status
     * Check current verification status of the authenticated professional.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getVerificationStatus(
            @AuthenticationPrincipal User user) {

        ProfessionalProfile profile = profileRepository.findByUser_Id(user.getId())
            .orElse(null);

        return ResponseEntity.ok(Map.of(
            "verificationStatus", user.getVerificationStatus().name(),
            "hasIdDocument", profile != null && profile.getIdDocumentUrl() != null,
            "hasCertification", profile != null && profile.getCertificationUrl() != null
        ));
    }
}
