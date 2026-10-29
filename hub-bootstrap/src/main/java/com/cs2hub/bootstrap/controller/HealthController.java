package com.cs2hub.bootstrap.controller;

import com.cs2hub.bootstrap.dto.HealthResponseDto;
import com.cs2hub.common.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Health & Diagnostics", description = "Endpoints for platform readiness, health monitoring, and system metrics")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String appVersion;

    public HealthController(@Value("${app.version:0.0.1-SNAPSHOT}") String appVersion) {
        this.appVersion = appVersion;
    }

    @Operation(
            summary = "Service Health Check",
            description = "Returns current health status and operational indicators of the CS2-SuperHub platform."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Application is operational and healthy",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service is unavailable or degraded"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<HealthResponseDto>> checkHealth() {
        Map<String, String> modules = Map.of(
                "hub-identity", "UP",
                "hub-loadouts", "UP",
                "hub-minigames", "UP",
                "hub-stratboard", "UP",
                "hub-matches", "UP"
        );

        HealthResponseDto health = HealthResponseDto.up(appVersion, modules);
        return ResponseEntity.ok(ApiResponseDto.ok("CS2-SuperHub modular monolith is healthy", health));
    }
}
