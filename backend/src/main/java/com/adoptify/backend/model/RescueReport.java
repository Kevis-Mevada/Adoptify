package com.adoptify.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_reports")
public class RescueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User reporter;

    private String reporterName;
    private String reporterPhone;
    private String animalType;
    private Integer animalCount;

    @Enumerated(EnumType.STRING)
    private AnimalCondition animalCondition;

    @Enumerated(EnumType.STRING)
    private EmergencyLevel emergencyLevel;

    @Column(length = 500)
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    @Column(length = 255)
    private String landmark;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String images; // comma separated URLs

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RescueStatus status = RescueStatus.PENDING;

    @Builder.Default
    private Boolean isImageVerified = false;
    private Double imageVerificationScore;
    @Column(length = 1000)
    private String imageVerificationMessage;

    private String adminRemarks;

    @Min(1)
    @Max(5)
    private Integer rating;
    private String ratingRemarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "rescueReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<RescueResponse> rescueResponses = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
