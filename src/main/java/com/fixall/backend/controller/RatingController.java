package com.fixall.backend.controller;

import com.fixall.backend.dto.request.CreateRatingRequest;
import com.fixall.backend.dto.response.RatingResponse;
import com.fixall.backend.model.User;
import com.fixall.backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // POST /api/ratings — Submit a rating for a completed job
    @PostMapping
    public ResponseEntity<RatingResponse> submitRating(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRatingRequest request) {
        return ResponseEntity.ok(ratingService.submitRating(user.getId(), request));
    }

    // GET /api/ratings/job/{jobId} — Get the rating for a specific job
    @GetMapping("/job/{jobId}")
    public ResponseEntity<RatingResponse> getRatingForJob(@PathVariable String jobId) {
        return ResponseEntity.ok(ratingService.getRatingForJob(jobId));
    }
}
