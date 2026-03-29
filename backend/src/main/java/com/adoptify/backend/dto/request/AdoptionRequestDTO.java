package com.adoptify.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdoptionRequestDTO {
    @NotNull
    private Long animalId;

    private Boolean hasExperience;
    private Boolean hasOtherPets;
    private Boolean hasYard;
    private String livingSituation;
    private Integer dailyHoursAlone;
    private String reasonForAdoption;
    private String additionalNotes;
    private String adopterContactPhone;
    private String adopterContactEmail;
    private LocalDate preferredMeetingDate;
    private String meetingAddress;
}
