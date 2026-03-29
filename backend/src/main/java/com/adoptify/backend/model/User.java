package com.adoptify.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private String organizationName;
    private String licenseNumber;

    @Column(columnDefinition = "boolean default false")
    private Boolean isVerified;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean rescueEnabled = false; // Restricted to verified NGOs only

    private String profileImage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    @Column(columnDefinition = "boolean default true")
    private Boolean isActive;

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
