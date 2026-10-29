package com.cs2hub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard API response wrapper")
public record ApiResponseDto<T>(
        @Schema(description = "Indicates if the request was successful", example = "true")
        boolean success,

        @Schema(description = "Response message or summary", example = "Operation completed successfully")
        String message,

        @Schema(description = "Payload data")
        T data,

        @Schema(description = "Timestamp when the response was generated")
        Instant timestamp
) {
    public static <T> ApiResponseDto<T> ok(T data) {
        return new ApiResponseDto<>(true, "Success", data, Instant.now());
    }

    public static <T> ApiResponseDto<T> ok(String message, T data) {
        return new ApiResponseDto<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponseDto<T> failure(String message) {
        return new ApiResponseDto<>(false, message, null, Instant.now());
    }
}
