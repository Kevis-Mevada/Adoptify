package com.adoptify.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    private String breed;
    private Integer ageYears;
    private Integer ageMonths;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private AnimalSize size;

    private String color;

    @Column(length = 1000)
    private String description;

    private String healthStatus;
    private Boolean vaccinated;
    private Boolean dewormed;
    private Boolean neutered;
    private String specialNeeds;
    private String behaviorNotes;
    private Boolean goodWithKids;
    private Boolean goodWithPets;
    private BigDecimal adoptionFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdoptionStatus status = AdoptionStatus.AVAILABLE;

    @Builder.Default
    private Integer viewsCount = 0;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnimalImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdoptionRequest> adoptionRequests = new ArrayList<>();

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
