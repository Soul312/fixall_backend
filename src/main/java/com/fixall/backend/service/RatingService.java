package com.fixall.backend.service;

import com.fixall.backend.dto.request.CreateRatingRequest;
import com.fixall.backend.dto.response.RatingResponse;
import com.fixall.backend.exception.*;
import com.fixall.backend.model.Job;
import com.fixall.backend.model.Rating;
import com.fixall.backend.model.User;
import com.fixall.backend.model.enums.JobStatus;
import com.fixall.backend.repository.JobRepository;
import com.fixall.backend.repository.RatingRepository;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponse submitRating(String clientId, CreateRatingRequest req) {
        Job job = jobRepository.findById(req.getJobId())
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + req.getJobId()));

        // Only the client who owns the job can rate
        if (!job.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the client of this job can submit a rating");
        }

        // Job must be completed
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BadRequestException("Cannot rate a job that is not completed");
        }

        // Prevent duplicate ratings
        if (ratingRepository.findByJob_Id(req.getJobId()).isPresent()) {
            throw new BadRequestException("This job has already been rated");
        }

        User professional = userRepository.findById(req.getProfessionalId())
            .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        Rating rating = Rating.builder()
            .job(job)
            .professional(professional)
            .score(req.getScore())
            .comment(req.getComment())
            .build();

        Rating saved = ratingRepository.save(rating);
        return toResponse(saved);
    }

    public RatingResponse getRatingForJob(String jobId) {
        Rating rating = ratingRepository.findByJob_Id(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("No rating found for job: " + jobId));
        return toResponse(rating);
    }

    private RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
            .id(rating.getId())
            .jobId(rating.getJob().getId())
            .professionalId(rating.getProfessional().getId())
            .professionalName(rating.getProfessional().getFullName())
            .score(rating.getScore())
            .comment(rating.getComment())
            .createdAt(rating.getCreatedAt())
            .build();
    }
}
