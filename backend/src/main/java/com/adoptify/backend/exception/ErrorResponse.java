package com.adoptify.backend.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
