package com.fixall.backend.repository;

import com.fixall.backend.model.ProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, String> {
    Optional<ProfessionalProfile> findByUser_Id(String userId);
}


