package com.adoptify.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "adoption_requests")
public class AdoptionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adopter_id", nullable = false)
    private User adopter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus requestStatus = RequestStatus.PENDING;

    private Boolean hasExperience;
    private Boolean hasOtherPets;
    private Boolean hasYard;

    @Enumerated(EnumType.STRING)
    private LivingSituation livingSituation;

    private Integer dailyHoursAlone;

    @Column(length = 1000)
    private String reasonForAdoption;

    @Column(length = 1000)
    private String additionalNotes;

    private String adopterContactPhone;
    private String adopterContactEmail;
    private LocalDate preferredMeetingDate;
    private String meetingAddress;
    private String rejectionReason;

    @Builder.Default
    private Boolean viewedByOwner = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
