package com.cs2hub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error response structure")
public record ErrorResponseDto(
        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Error code identifier", example = "VALIDATION_FAILED")
        String error,

        @Schema(description = "Detailed error message", example = "Invalid request parameters")
        String message,

        @Schema(description = "List of specific field error details, if any")
        List<String> details,

        @Schema(description = "Timestamp when the error occurred")
        Instant timestamp
) {
    public static ErrorResponseDto of(int status, String error, String message) {
        return new ErrorResponseDto(status, error, message, List.of(), Instant.now());
    }

    public static ErrorResponseDto of(int status, String error, String message, List<String> details) {
        return new ErrorResponseDto(status, error, message, details, Instant.now());
    }
}
