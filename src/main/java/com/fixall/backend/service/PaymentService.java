package com.fixall.backend.service;

import com.fixall.backend.dto.request.ConfirmPaymentRequest;
import com.fixall.backend.dto.request.CreatePaymentIntentRequest;
import com.fixall.backend.dto.response.PaymentIntentResponse;
import com.fixall.backend.exception.BadRequestException;
import com.fixall.backend.exception.ForbiddenException;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.Job;
import com.fixall.backend.model.enums.JobStatus;
import com.fixall.backend.model.enums.PaymentStatus;
import com.fixall.backend.repository.JobRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final JobRepository jobRepository;

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecret;
        log.info("Stripe API initialized");
    }

    /**
     * Create a Stripe PaymentIntent for a completed job.
     * Returns the clientSecret the frontend needs to confirm the payment.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(String clientId, CreatePaymentIntentRequest req) {
        Job job = jobRepository.findById(req.getJobId())
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + req.getJobId()));

        // Only the client who owns the job can pay
        if (!job.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the client of this job can initiate payment");
        }

        // Job must be accepted or completed before payment
        if (job.getStatus() != JobStatus.ACCEPTED && job.getStatus() != JobStatus.COMPLETED) {
            throw new BadRequestException("Job must be ACCEPTED or COMPLETED before payment can be made");
        }

        // Prevent double payment
        if (job.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This job has already been paid");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(req.getAmountCents())
                .setCurrency("mad")  // Moroccan Dirham
                .putMetadata("jobId", job.getId())
                .putMetadata("clientId", clientId)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Store the intent ID on the job
            job.setStripePaymentIntentId(intent.getId());
            job.setActualPrice(BigDecimal.valueOf(req.getAmountCents()).divide(BigDecimal.valueOf(100)));
            jobRepository.save(job);

            log.info("PaymentIntent created: {} for job {}", intent.getId(), job.getId());

            return PaymentIntentResponse.builder()
                .clientSecret(intent.getClientSecret())
                .paymentIntentId(intent.getId())
                .jobId(job.getId())
                .amountCents(req.getAmountCents())
                .currency("mad")
                .build();

        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent", e);
            throw new BadRequestException("Payment failed: " + e.getMessage());
        }
    }

    /**
     * Confirm payment was successful (called after frontend confirms with Stripe).
     * Updates the job's payment status to PAID.
     */
    @Transactional
    public void confirmPayment(String clientId, ConfirmPaymentRequest req) {
        Job job = jobRepository.findById(req.getJobId())
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + req.getJobId()));

        if (!job.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the client of this job can confirm payment");
        }

        try {
            // Verify with Stripe that the payment was successful
            PaymentIntent intent = PaymentIntent.retrieve(req.getPaymentIntentId());

            if (!"succeeded".equals(intent.getStatus())) {
                throw new BadRequestException("Payment has not been completed. Status: " + intent.getStatus());
            }

            job.setPaymentStatus(PaymentStatus.PAID);
            job.setStripePaymentIntentId(req.getPaymentIntentId());
            jobRepository.save(job);

            log.info("Payment confirmed for job {} (intent: {})", job.getId(), req.getPaymentIntentId());

        } catch (StripeException e) {
            log.error("Stripe error confirming payment", e);
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }
    }
}
