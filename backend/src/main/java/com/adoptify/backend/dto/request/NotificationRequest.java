package com.adoptify.backend.dto.request;

import com.adoptify.backend.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull
    private Long userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private Long relatedId;
    private boolean sendEmail;
}
