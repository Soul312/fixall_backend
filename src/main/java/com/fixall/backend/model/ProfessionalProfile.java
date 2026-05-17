package com.fixall.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "professional_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ElementCollection
    @CollectionTable(name = "professional_categories", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "category")
    private List<String> categories;

    @Column(name = "id_document_url")
    private String idDocumentUrl;

    @Column(name = "certification_url")
    private String certificationUrl;

    private Double ratingAverage;
    private Integer totalJobs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}

