package com.cs2hub.bootstrap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Application health status response")
public record HealthResponseDto(
        @Schema(description = "Overall service status", example = "UP")
        String status,

        @Schema(description = "Application name", example = "cs2-superhub")
        String application,

        @Schema(description = "Current application version", example = "0.0.1-SNAPSHOT")
        String version,

        @Schema(description = "System timestamp when health was queried")
        Instant timestamp,

        @Schema(description = "Status of individual subsystem components")
        Map<String, String> components
) {
    public static HealthResponseDto up(String version, Map<String, String> components) {
        return new HealthResponseDto("UP", "cs2-superhub", version, Instant.now(), components);
    }
}
