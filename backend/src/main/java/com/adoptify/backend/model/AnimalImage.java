package com.adoptify.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "animal_images")
public class AnimalImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private String imageUrl;

    private Boolean isPrimary;

    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
