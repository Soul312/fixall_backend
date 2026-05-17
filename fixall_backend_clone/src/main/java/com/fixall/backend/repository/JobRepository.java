package com.fixall.backend.repository;

import com.fixall.backend.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByClient_Id(String clientId);
    List<Job> findByProfessional_Id(String proId);

    // Example radius query using database function calculate_distance_km(lat1, lon1, lat2, lon2)
    @Query("SELECT j FROM Job j WHERE j.status = 'REQUESTED' AND function('calculate_distance_km', :lat, :lon, j.latitude, j.longitude) <= :radius")
    List<Job> findAvailableWithinRadius(@Param("lat") double lat, @Param("lon") double lon, @Param("radius") double radiusKm);
}

