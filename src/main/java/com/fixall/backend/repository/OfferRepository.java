package com.fixall.backend.repository;

import com.fixall.backend.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, String> {
    List<Offer> findByJob_Id(String jobId);
    List<Offer> findByProfessional_Id(String professionalId);
    Optional<Offer> findByJob_IdAndProfessional_Id(String jobId, String professionalId);
}
