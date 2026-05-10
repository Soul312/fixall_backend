package com.fixall.backend.repository;

import com.fixall.backend.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, String> {
    Optional<Rating> findByJob_Id(String jobId);
}

