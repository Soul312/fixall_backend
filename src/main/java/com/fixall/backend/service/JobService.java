package com.fixall.backend.service;

import com.fixall.backend.dto.request.CreateJobRequest;
import com.fixall.backend.dto.response.JobResponse;
import com.fixall.backend.exception.*;
import com.fixall.backend.model.Job;
import com.fixall.backend.model.User;
import com.fixall.backend.model.enums.JobStatus;
import com.fixall.backend.model.enums.UserRole;
import com.fixall.backend.repository.JobRepository;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ── Create ────────────────────────────────────────────────────
    @Transactional
    public JobResponse createJob(String clientId, CreateJobRequest req) {
        User client = userRepository.findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (client.getRole() != UserRole.CLIENT) {
            throw new ForbiddenException("Only clients can create service requests");
        }

        Job job = Job.builder()
            .title(req.getTitle())
            .description(req.getDescription())
            .category(req.getCategory())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .estimatedPrice(req.getEstimatedPrice())
            .status(JobStatus.REQUESTED)
            .client(client)
            .build();

        return toResponse(jobRepository.save(job));
    }

    // ── Read ──────────────────────────────────────────────────────
    public List<JobResponse> getMyJobs(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Job> jobs;
        if (user.getRole() == UserRole.PROFESSIONAL) {
            jobs = jobRepository.findByProfessional_Id(userId);
        } else {
            jobs = jobRepository.findByClient_Id(userId);
        }
        return jobs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<JobResponse> getAvailableJobs(double lat, double lng, double radiusKm) {
        // Use the Haversine-based repository query
        return jobRepository.findAvailableWithinRadius(lat, lng, radiusKm)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public JobResponse getJobById(String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return toResponse(job);
    }

    // ── Accept ────────────────────────────────────────────────────
    @Transactional
    public JobResponse acceptJob(String jobId, String proUserId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.REQUESTED) {
            throw new BadRequestException("Job is not in REQUESTED status");
        }

        User pro = userRepository.findById(proUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (pro.getRole() != UserRole.PROFESSIONAL) {
            throw new ForbiddenException("Only professionals can accept jobs");
        }

        job.setProfessional(pro);
        job.setStatus(JobStatus.ACCEPTED);
        job.setAcceptedAt(LocalDateTime.now());

        JobResponse response = toResponse(jobRepository.save(job));

        // Notify the client that a professional accepted their request
        notificationService.notifyJobStatusChange(
            job.getClient().getFcmToken(),
            job.getId(),
            job.getTitle(),
            "ACCEPTED",
            pro.getFullName() + " accepted your request: " + job.getTitle()
        );

        return response;
    }

    // ── Complete ──────────────────────────────────────────────────
    @Transactional
    public JobResponse completeJob(String jobId, String proUserId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.ACCEPTED && job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job must be ACCEPTED or IN_PROGRESS to complete");
        }

        if (job.getProfessional() == null || !job.getProfessional().getId().equals(proUserId)) {
            throw new ForbiddenException("Only the assigned professional can complete this job");
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        JobResponse response = toResponse(jobRepository.save(job));

        // Notify the client that the job has been completed
        notificationService.notifyJobStatusChange(
            job.getClient().getFcmToken(),
            job.getId(),
            job.getTitle(),
            "COMPLETED",
            "Your job \"" + job.getTitle() + "\" has been marked as complete"
        );

        return response;
    }

    // ── Cancel ────────────────────────────────────────────────────
    @Transactional
    public JobResponse cancelJob(String jobId, String userId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        boolean isClient = job.getClient().getId().equals(userId);
        boolean isPro = job.getProfessional() != null && job.getProfessional().getId().equals(userId);

        if (!isClient && !isPro) {
            throw new ForbiddenException("You are not involved in this job");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed job");
        }

        job.setStatus(JobStatus.CANCELLED);
        return toResponse(jobRepository.save(job));
    }

    // ── Mapper ────────────────────────────────────────────────────
    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
            .id(job.getId())
            .title(job.getTitle())
            .description(job.getDescription())
            .category(job.getCategory())
            .status(job.getStatus().name())
            .clientId(job.getClient().getId())
            .clientName(job.getClient().getFullName())
            .professionalId(job.getProfessional() != null ? job.getProfessional().getId() : null)
            .professionalName(job.getProfessional() != null ? job.getProfessional().getFullName() : null)
            .latitude(job.getLatitude())
            .longitude(job.getLongitude())
            .estimatedPrice(job.getEstimatedPrice())
            .actualPrice(job.getActualPrice())
            .paymentStatus(job.getPaymentStatus() != null ? job.getPaymentStatus().name() : null)
            .stripePaymentIntentId(job.getStripePaymentIntentId())
            .createdAt(job.getCreatedAt())
            .acceptedAt(job.getAcceptedAt())
            .completedAt(job.getCompletedAt())
            .build();
    }
}
