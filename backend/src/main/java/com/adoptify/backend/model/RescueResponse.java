package com.adoptify.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rescue_responses")
public class RescueResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_report_id", nullable = false)
    private RescueReport rescueReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponderType responderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponseStatus responseStatus;

    private String responseMessage;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualArrivalTime;
    private String rescueNotes;

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
