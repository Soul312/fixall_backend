package com.fixall.backend.model.enums;

/**
 * Lifecycle of a price negotiation between a professional and a client for a job.
 *
 * A professional opens an offer (AWAITING_CLIENT). The client may accept it,
 * decline it, or counter (AWAITING_PRO). The professional may then accept the
 * counter, decline, or counter back (AWAITING_CLIENT) — and so on until one side
 * accepts (ACCEPTED) or declines (DECLINED). Multiple professionals can have
 * competing offers on the same job; accepting one declines the rest.
 */
public enum OfferStatus {
    AWAITING_CLIENT, // professional proposed/countered — client's turn to respond
    AWAITING_PRO,    // client countered — professional's turn to respond
    ACCEPTED,
    DECLINED
}
