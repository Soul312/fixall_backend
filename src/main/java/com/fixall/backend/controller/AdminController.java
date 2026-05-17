package com.fixall.backend.controller;

import com.fixall.backend.exception.BadRequestException;
import com.fixall.backend.exception.ForbiddenException;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.Job;
import com.fixall.backend.model.User;
import com.fixall.backend.model.enums.JobStatus;
import com.fixall.backend.model.enums.UserRole;
import com.fixall.backend.model.enums.VerificationStatus;
import com.fixall.backend.repository.JobRepository;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    // ── Guard ────────────────────────────────────────────────────
    private void requireAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  USER MANAGEMENT
    // ══════════════════════════════════════════════════════════════

    /** GET /api/admin/users — list all users */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(@AuthenticationPrincipal User admin) {
        requireAdmin(admin);
        List<Map<String, Object>> users = userRepository.findAll().stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "role", u.getRole().name(),
                "verificationStatus", u.getVerificationStatus().name(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /** GET /api/admin/users/{id} — get user details */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @AuthenticationPrincipal User admin,
            @PathVariable String id) {
        requireAdmin(admin);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "role", user.getRole().name(),
            "verificationStatus", user.getVerificationStatus().name(),
            "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    /** PATCH /api/admin/users/{id}/role — change user role */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> changeUserRole(
            @AuthenticationPrincipal User admin,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        requireAdmin(admin);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        String newRole = body.get("role");
        if (newRole == null) throw new BadRequestException("Missing 'role' in body");
        
        try {
            user.setRole(UserRole.valueOf(newRole));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + newRole);
        }
        
        userRepository.save(user);
        log.info("Admin {} changed role of user {} to {}", admin.getId(), id, newRole);
        return ResponseEntity.ok(Map.of("message", "Role updated to " + newRole));
    }

    // ══════════════════════════════════════════════════════════════
    //  VERIFICATION MANAGEMENT
    // ══════════════════════════════════════════════════════════════

    /** GET /api/admin/verifications/pending — list users awaiting verification */
    @GetMapping("/verifications/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingVerifications(
            @AuthenticationPrincipal User admin) {
        requireAdmin(admin);
        List<Map<String, Object>> pending = userRepository.findAll().stream()
            .filter(u -> u.getVerificationStatus() == VerificationStatus.PENDING && u.getRole() == UserRole.PROFESSIONAL)
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "verificationStatus", u.getVerificationStatus().name()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    /** PATCH /api/admin/users/{id}/verify — approve or reject a professional */
    @PatchMapping("/users/{id}/verify")
    public ResponseEntity<Map<String, String>> verifyProfessional(
            @AuthenticationPrincipal User admin,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        requireAdmin(admin);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        String action = body.get("action"); // "APPROVE" or "REJECT"
        if (action == null) throw new BadRequestException("Missing 'action' in body (APPROVE or REJECT)");

        switch (action.toUpperCase()) {
            case "APPROVE":
                user.setVerificationStatus(VerificationStatus.VERIFIED);
                break;
            case "REJECT":
                user.setVerificationStatus(VerificationStatus.REJECTED);
                break;
            default:
                throw new BadRequestException("Invalid action: " + action + ". Use APPROVE or REJECT.");
        }

        userRepository.save(user);
        log.info("Admin {} {} professional {}", admin.getId(), action, id);
        return ResponseEntity.ok(Map.of("message", "Professional " + action.toLowerCase() + "d successfully"));
    }

    // ══════════════════════════════════════════════════════════════
    //  JOB MANAGEMENT
    // ══════════════════════════════════════════════════════════════

    /** GET /api/admin/jobs — list all jobs with optional status filter */
    @GetMapping("/jobs")
    public ResponseEntity<List<Map<String, Object>>> getAllJobs(
            @AuthenticationPrincipal User admin,
            @RequestParam(required = false) String status) {
        requireAdmin(admin);
        List<Job> jobs = jobRepository.findAll();
        
        if (status != null && !status.isBlank()) {
            try {
                JobStatus filter = JobStatus.valueOf(status.toUpperCase());
                jobs = jobs.stream()
                    .filter(j -> j.getStatus() == filter)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status filter: " + status);
            }
        }

        List<Map<String, Object>> result = jobs.stream()
            .map(j -> Map.<String, Object>of(
                "id", j.getId(),
                "title", j.getTitle(),
                "category", j.getCategory() != null ? j.getCategory() : "",
                "status", j.getStatus().name(),
                "clientName", j.getClient().getFullName() != null ? j.getClient().getFullName() : "",
                "professionalName", j.getProfessional() != null && j.getProfessional().getFullName() != null
                    ? j.getProfessional().getFullName() : "—",
                "createdAt", j.getCreatedAt() != null ? j.getCreatedAt().toString() : "",
                "paymentStatus", j.getPaymentStatus() != null ? j.getPaymentStatus().name() : "PENDING"
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** PATCH /api/admin/jobs/{id}/cancel — force-cancel a job */
    @PatchMapping("/jobs/{id}/cancel")
    public ResponseEntity<Map<String, String>> forceCancelJob(
            @AuthenticationPrincipal User admin,
            @PathVariable String id) {
        requireAdmin(admin);
        Job job = jobRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed job");
        }

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        
        log.info("Admin {} force-cancelled job {}", admin.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Job cancelled by admin"));
    }

    // ══════════════════════════════════════════════════════════════
    //  DASHBOARD STATS
    // ══════════════════════════════════════════════════════════════

    /** GET /api/admin/stats — overview statistics */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@AuthenticationPrincipal User admin) {
        requireAdmin(admin);
        
        List<User> allUsers = userRepository.findAll();
        List<Job> allJobs = jobRepository.findAll();

        long totalUsers = allUsers.size();
        long totalClients = allUsers.stream().filter(u -> u.getRole() == UserRole.CLIENT).count();
        long totalPros = allUsers.stream().filter(u -> u.getRole() == UserRole.PROFESSIONAL).count();
        long pendingVerifications = allUsers.stream()
            .filter(u -> u.getVerificationStatus() == VerificationStatus.PENDING && u.getRole() == UserRole.PROFESSIONAL)
            .count();

        long totalJobs = allJobs.size();
        long openJobs = allJobs.stream().filter(j -> j.getStatus() == JobStatus.REQUESTED).count();
        long activeJobs = allJobs.stream().filter(j -> j.getStatus() == JobStatus.ACCEPTED || j.getStatus() == JobStatus.IN_PROGRESS).count();
        long completedJobs = allJobs.stream().filter(j -> j.getStatus() == JobStatus.COMPLETED).count();

        return ResponseEntity.ok(Map.of(
            "totalUsers", totalUsers,
            "totalClients", totalClients,
            "totalPros", totalPros,
            "pendingVerifications", pendingVerifications,
            "totalJobs", totalJobs,
            "openJobs", openJobs,
            "activeJobs", activeJobs,
            "completedJobs", completedJobs
        ));
    }
}
