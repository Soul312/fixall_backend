package com.fixall.backend.controller;

import com.fixall.backend.dto.request.CreateOfferRequest;
import com.fixall.backend.dto.request.OfferAmountRequest;
import com.fixall.backend.dto.response.OfferResponse;
import com.fixall.backend.model.User;
import com.fixall.backend.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    // POST /api/offers — professional proposes a price on an open job
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOfferRequest req) {
        return ResponseEntity.ok(offerService.createOffer(user.getId(), req.getJobId(), req.getAmount()));
    }

    // GET /api/offers/job/{jobId} — offers on a job (client: all; pro: own)
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<OfferResponse>> getOffersForJob(
            @AuthenticationPrincipal User user,
            @PathVariable String jobId) {
        return ResponseEntity.ok(offerService.getOffersForJob(user.getId(), jobId));
    }

    // GET /api/offers/my — offers the current professional has made
    @GetMapping("/my")
    public ResponseEntity<List<OfferResponse>> getMyOffers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(offerService.getMyOffers(user.getId()));
    }

    // PATCH /api/offers/{id}/accept — accept the current proposal (party whose turn it is)
    @PatchMapping("/{id}/accept")
    public ResponseEntity<OfferResponse> accept(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(offerService.acceptOffer(user.getId(), id));
    }

    // PATCH /api/offers/{id}/decline — decline the offer
    @PatchMapping("/{id}/decline")
    public ResponseEntity<OfferResponse> decline(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(offerService.declineOffer(user.getId(), id));
    }

    // PATCH /api/offers/{id}/counter — propose a different amount
    @PatchMapping("/{id}/counter")
    public ResponseEntity<OfferResponse> counter(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody OfferAmountRequest req) {
        return ResponseEntity.ok(offerService.counterOffer(user.getId(), id, req.getAmount()));
    }
}
