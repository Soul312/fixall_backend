package com.fixall.backend.controller;

import com.fixall.backend.dto.request.ConfirmPaymentRequest;
import com.fixall.backend.dto.request.CreatePaymentIntentRequest;
import com.fixall.backend.dto.response.PaymentIntentResponse;
import com.fixall.backend.model.User;
import com.fixall.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${stripe.publishable}")
    private String stripePublishable;

    /**
     * GET /api/payments/config
     * Returns the Stripe publishable key so the frontend can initialize Stripe.js.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishable));
    }

    /**
     * POST /api/payments/create-intent
     * Client creates a Stripe PaymentIntent for an accepted/completed job.
     * Returns the clientSecret needed by the frontend Stripe SDK.
     */
    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(user.getId(), request));
    }

    /**
     * POST /api/payments/confirm
     * Client confirms that payment succeeded after the Stripe SDK flow.
     * Marks the job as PAID.
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        paymentService.confirmPayment(user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Payment confirmed successfully"));
    }
}
