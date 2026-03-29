package com.adoptify.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDTO {

    private Long id;
    private String name;
    private String species;
    private String breed;
    private Integer ageYears;
    private Integer ageMonths;
    private String gender;
    private String size;
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
    private String status;
    private Integer viewsCount;
    private List<AnimalImageDTO> images;
    private OwnerInfoDTO owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfoDTO {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String city;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnimalImageDTO {
        private Long id;
        private String imageUrl;
        private Boolean isPrimary;
    }
}
