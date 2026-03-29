package com.adoptify.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdoptionResponseDTO {
    private Long id;
    private Long animalId;
    private String animalName;
    private String animalImage;
    private String animalBreed;
    private String animalAge;
    private String animalGender;
    private String animalSize;
    private String animalColor;
    private Long adopterId;
    private String adopterName;
    private String adopterPhone;  // Only show if request is APPROVED
    private String adopterEmail;  // Only show if request is APPROVED
    private Long ownerId;
    private String ownerName;
    private String ownerPhone;    // Only show if request is APPROVED
    private String ownerEmail;    // Only show if request is APPROVED
    private String ownerAddress;  // Only show if request is APPROVED
    private String requestStatus;
    private String reasonForAdoption;
    private String livingSituation;
    private Boolean hasYard;
    private Boolean hasOtherPets;
    private Boolean hasExperience;
    private Integer dailyHoursAlone;
    private LocalDate preferredMeetingDate;
    private String meetingAddress;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
