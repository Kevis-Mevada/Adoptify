package com.adoptify.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String fullName;
    
    @NotBlank
    private String phone;
    
    @NotBlank
    private String city;
    
    @NotBlank
    private String address;

    private String organizationName;
}
