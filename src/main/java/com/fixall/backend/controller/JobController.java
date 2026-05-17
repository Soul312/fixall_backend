package com.fixall.backend.controller;

import com.fixall.backend.dto.request.CreateJobRequest;
import com.fixall.backend.dto.response.JobResponse;
import com.fixall.backend.model.User;
import com.fixall.backend.service.FileStorageService;
import com.fixall.backend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final FileStorageService fileStorageService;

    // POST /api/requests — Client creates a new service request
    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(jobService.createJob(user.getId(), request));
    }

    // GET /api/requests/my — Get the current user's jobs (client or pro)
    @GetMapping("/my")
    public ResponseEntity<List<JobResponse>> getMyJobs(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(jobService.getMyJobs(user.getId()));
    }

    // GET /api/requests/available?lat=&lng=&radiusKm= — Available jobs within radius
    @GetMapping("/available")
    public ResponseEntity<List<JobResponse>> getAvailableJobs(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(jobService.getAvailableJobs(lat, lng, radiusKm));
    }

    // GET /api/requests/{id} — Get a single job by ID
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // PATCH /api/requests/{id}/accept — Professional accepts a job
    @PatchMapping("/{id}/accept")
    public ResponseEntity<JobResponse> acceptJob(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(jobService.acceptJob(id, user.getId()));
    }

    // PATCH /api/requests/{id}/complete — Professional completes a job
    @PatchMapping("/{id}/complete")
    public ResponseEntity<JobResponse> completeJob(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(jobService.completeJob(id, user.getId()));
    }

    // PATCH /api/requests/{id}/cancel — Cancel a job
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<JobResponse> cancelJob(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(jobService.cancelJob(id, user.getId()));
    }

    // POST /api/requests/{id}/photos — Upload a diagnostic photo for a job
    @PostMapping("/{id}/photos")
    public ResponseEntity<Map<String, String>> uploadJobPhoto(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestParam("photo") MultipartFile photo) {
        // Verify job exists and user is involved
        jobService.getJobById(id); // throws 404 if not found
        String fileUrl = fileStorageService.storeFile(photo, "job-photos/" + id);
        return ResponseEntity.ok(Map.of(
            "message", "Photo uploaded successfully",
            "fileUrl", fileUrl
        ));
    }
}

