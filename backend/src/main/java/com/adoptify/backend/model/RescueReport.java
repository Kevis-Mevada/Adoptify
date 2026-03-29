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
    private User reporter;

    private String reporterName;
    private String reporterPhone;
    private String animalType;
    private Integer animalCount;

    @Enumerated(EnumType.STRING)
    private AnimalCondition animalCondition;

    @Enumerated(EnumType.STRING)
    private EmergencyLevel emergencyLevel;

    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private String landmark;

    @Column(length = 1000)
    private String description;

    private String images; // comma separated URLs

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RescueStatus status = RescueStatus.PENDING;

    private String adminRemarks;

    @Min(1)
    @Max(5)
    private Integer rating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

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
