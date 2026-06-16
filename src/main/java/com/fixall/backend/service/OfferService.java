package com.fixall.backend.service;

import com.fixall.backend.dto.response.OfferResponse;
import com.fixall.backend.exception.BadRequestException;
import com.fixall.backend.exception.ForbiddenException;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.Job;
import com.fixall.backend.model.Offer;
import com.fixall.backend.model.User;
import com.fixall.backend.model.enums.JobStatus;
import com.fixall.backend.model.enums.OfferStatus;
import com.fixall.backend.model.enums.UserRole;
import com.fixall.backend.repository.JobRepository;
import com.fixall.backend.repository.OfferRepository;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Price-negotiation logic. Professionals make competing offers on open jobs;
 * either party can counter; accepting an offer assigns the job and declines the
 * rest. The job stays REQUESTED (visible to all pros) until an offer is accepted.
 */
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ── Create / re-open an offer (professional) ──────────────────
    @Transactional
    public OfferResponse createOffer(String proId, String jobId, BigDecimal amount) {
        User pro = userRepository.findById(proId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (pro.getRole() != UserRole.PROFESSIONAL) {
            throw new ForbiddenException("Only professionals can make offers");
        }

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        if (job.getStatus() != JobStatus.REQUESTED) {
            throw new BadRequestException("This job is no longer open for offers");
        }
        if (job.getClient().getId().equals(proId)) {
            throw new ForbiddenException("You cannot make an offer on your own request");
        }

        // One offer row per (job, pro): reuse it if the pro already negotiated here.
        Offer offer = offerRepository.findByJob_IdAndProfessional_Id(jobId, proId)
            .orElseGet(() -> Offer.builder().job(job).professional(pro).build());

        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            throw new BadRequestException("This offer was already accepted");
        }

        offer.setAmount(amount);
        offer.setStatus(OfferStatus.AWAITING_CLIENT);
        Offer saved = offerRepository.save(offer);

        notify(job.getClient(), job, "New offer",
            pro.getFullName() + " offered " + amount + " for: " + job.getTitle());

        return toResponse(saved);
    }

    // ── Counter (either party) ────────────────────────────────────
    @Transactional
    public OfferResponse counterOffer(String userId, String offerId, BigDecimal amount) {
        Offer offer = loadOpen(offerId);
        Job job = offer.getJob();

        boolean isClient = job.getClient().getId().equals(userId);
        boolean isPro = offer.getProfessional().getId().equals(userId);

        if (isClient) {
            if (offer.getStatus() != OfferStatus.AWAITING_CLIENT) {
                throw new BadRequestException("It is not your turn to respond to this offer");
            }
            offer.setStatus(OfferStatus.AWAITING_PRO);
            offer.setAmount(amount);
            offerRepository.save(offer);
            notify(offer.getProfessional(), job, "Counter-offer",
                job.getClient().getFullName() + " countered with " + amount + " for: " + job.getTitle());
        } else if (isPro) {
            if (offer.getStatus() != OfferStatus.AWAITING_PRO) {
                throw new BadRequestException("It is not your turn to respond to this offer");
            }
            offer.setStatus(OfferStatus.AWAITING_CLIENT);
            offer.setAmount(amount);
            offerRepository.save(offer);
            notify(job.getClient(), job, "Counter-offer",
                offer.getProfessional().getFullName() + " countered with " + amount + " for: " + job.getTitle());
        } else {
            throw new ForbiddenException("You are not part of this negotiation");
        }
        return toResponse(offer);
    }

    // ── Accept (the party whose turn it is) ───────────────────────
    @Transactional
    public OfferResponse acceptOffer(String userId, String offerId) {
        Offer offer = loadOpen(offerId);
        Job job = offer.getJob();

        if (job.getStatus() != JobStatus.REQUESTED) {
            throw new BadRequestException("This job is no longer open");
        }

        boolean clientAccepting = job.getClient().getId().equals(userId)
            && offer.getStatus() == OfferStatus.AWAITING_CLIENT;
        boolean proAccepting = offer.getProfessional().getId().equals(userId)
            && offer.getStatus() == OfferStatus.AWAITING_PRO;

        if (!clientAccepting && !proAccepting) {
            throw new BadRequestException("It is not your turn to accept this offer");
        }

        // Finalize the negotiation.
        offer.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(offer);

        // Assign the job and lock in the agreed price.
        job.setProfessional(offer.getProfessional());
        job.setStatus(JobStatus.ACCEPTED);
        job.setAcceptedAt(LocalDateTime.now());
        job.setEstimatedPrice(offer.getAmount());
        jobRepository.save(job);

        // Decline every other offer on this job.
        offerRepository.findByJob_Id(job.getId()).forEach(other -> {
            if (!other.getId().equals(offer.getId()) && other.getStatus() != OfferStatus.DECLINED) {
                other.setStatus(OfferStatus.DECLINED);
                offerRepository.save(other);
                notify(other.getProfessional(), job, "Offer not selected",
                    "Another professional was selected for: " + job.getTitle());
            }
        });

        // Notify the counterpart of the agreement.
        if (clientAccepting) {
            notify(offer.getProfessional(), job, "Offer accepted",
                job.getClient().getFullName() + " accepted your offer for: " + job.getTitle());
        } else {
            notify(job.getClient(), job, "Offer accepted",
                offer.getProfessional().getFullName() + " accepted your price for: " + job.getTitle());
        }

        return toResponse(offer);
    }

    // ── Decline / withdraw (either party) ─────────────────────────
    @Transactional
    public OfferResponse declineOffer(String userId, String offerId) {
        Offer offer = loadOpen(offerId);
        Job job = offer.getJob();

        boolean isClient = job.getClient().getId().equals(userId);
        boolean isPro = offer.getProfessional().getId().equals(userId);
        if (!isClient && !isPro) {
            throw new ForbiddenException("You are not part of this negotiation");
        }

        offer.setStatus(OfferStatus.DECLINED);
        offerRepository.save(offer);

        User other = isClient ? offer.getProfessional() : job.getClient();
        notify(other, job, "Offer declined", "An offer was declined for: " + job.getTitle());
        return toResponse(offer);
    }

    // ── Reads ─────────────────────────────────────────────────────
    public List<OfferResponse> getOffersForJob(String userId, String jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        List<Offer> offers = offerRepository.findByJob_Id(jobId);
        boolean isClient = job.getClient().getId().equals(userId);

        // Client sees all offers; a professional sees only their own.
        return offers.stream()
            .filter(o -> isClient || o.getProfessional().getId().equals(userId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<OfferResponse> getMyOffers(String proId) {
        return offerRepository.findByProfessional_Id(proId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Offer loadOpen(String offerId) {
        Offer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));
        if (offer.getStatus() == OfferStatus.ACCEPTED || offer.getStatus() == OfferStatus.DECLINED) {
            throw new BadRequestException("This offer is already closed");
        }
        return offer;
    }

    private void notify(User recipient, Job job, String title, String message) {
        if (recipient == null) return;
        notificationService.notifyJobStatusChange(
            recipient.getFcmToken(), job.getId(), title, "OFFER", message);
    }

    private OfferResponse toResponse(Offer offer) {
        String awaiting = switch (offer.getStatus()) {
            case AWAITING_CLIENT -> "CLIENT";
            case AWAITING_PRO -> "PRO";
            default -> null;
        };
        return OfferResponse.builder()
            .id(offer.getId())
            .jobId(offer.getJob().getId())
            .jobTitle(offer.getJob().getTitle())
            .professionalId(offer.getProfessional().getId())
            .professionalName(offer.getProfessional().getFullName())
            .amount(offer.getAmount())
            .status(offer.getStatus().name())
            .awaitingParty(awaiting)
            .createdAt(offer.getCreatedAt())
            .updatedAt(offer.getUpdatedAt())
            .build();
    }
}
