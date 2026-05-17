package com.fixall.backend.repository;

import com.fixall.backend.model.Job;
import com.fixall.backend.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByClient_Id(String clientId);
    List<Job> findByProfessional_Id(String proId);
    List<Job> findByStatus(JobStatus status);

    /**
     * Pure Haversine radius search — no PostgreSQL extensions or custom functions needed.
     * Returns REQUESTED jobs within {@code radiusKm} km of (lat, lon).
     */
    @Query(value = """
        SELECT * FROM jobs j
        WHERE j.status = 'REQUESTED'
          AND ( 6371 * acos(
                  cos(radians(:lat)) * cos(radians(j.latitude))
                  * cos(radians(j.longitude) - radians(:lon))
                  + sin(radians(:lat)) * sin(radians(j.latitude))
              )) <= :radius
        ORDER BY ( 6371 * acos(
                  cos(radians(:lat)) * cos(radians(j.latitude))
                  * cos(radians(j.longitude) - radians(:lon))
                  + sin(radians(:lat)) * sin(radians(j.latitude))
              )) ASC
        """, nativeQuery = true)
    List<Job> findAvailableWithinRadius(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("radius") double radiusKm);
}

