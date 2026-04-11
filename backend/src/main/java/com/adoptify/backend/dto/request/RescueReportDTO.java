package com.adoptify.backend.dto.request;

import com.adoptify.backend.model.AnimalCondition;
import com.adoptify.backend.model.EmergencyLevel;
import com.adoptify.backend.model.RescueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescueReportDTO {

    private Long id;

    @NotBlank
    private String animalType;

    private Integer animalCount;

    @NotNull
    private AnimalCondition animalCondition;

    @NotNull
    private EmergencyLevel emergencyLevel;

    @NotBlank
    private String locationAddress;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String landmark;
    private String description;
    
    private String images; // For internal use/submission
    private List<String> imageUrls; // List of URLs for retrieval

    private String reporterName;
    private String reporterPhone;
    private boolean imagesRequired;

    private RescueStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ResponderInfo responder;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponderInfo {
        private String name;
        private String phone;
        private String organizationName;
        private LocalDateTime estimatedArrivalTime;
    }
}
