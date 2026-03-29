package com.adoptify.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRequestDTO {
    private Long requestId;

    @NotBlank
    private String rejectionReason;
}
