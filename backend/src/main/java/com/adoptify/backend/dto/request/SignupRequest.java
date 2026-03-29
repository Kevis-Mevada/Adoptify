package com.adoptify.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    @NotBlank
    private String role;

    private String organizationName;
    private String licenseNumber;
    
    private String city;
    private String address;
    private Double latitude;
    private Double longitude;
}
