package com.adoptify.backend.dto.request;

import com.adoptify.backend.model.AdoptionStatus;
import com.adoptify.backend.model.AnimalSize;
import com.adoptify.backend.model.Gender;
import com.adoptify.backend.model.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnimalRequest {

    @NotBlank
    private String name;

    @NotNull
    private Species species;

    private String breed;
    private Integer ageYears;
    private Integer ageMonths;
    private Gender gender;
    private AnimalSize size;
    private String color;
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
    private AdoptionStatus status;
}
