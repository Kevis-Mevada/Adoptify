package com.adoptify.backend.dto.request;

import com.adoptify.backend.model.AnimalCondition;
import com.adoptify.backend.model.EmergencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RescueReportDTO {

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
    private String images; // comma-separated URLs
    private String reporterName;
    private String reporterPhone;
}
