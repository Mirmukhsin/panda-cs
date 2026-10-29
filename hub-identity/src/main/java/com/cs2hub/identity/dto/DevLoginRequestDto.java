package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Development environment fast-login request")
public record DevLoginRequestDto(
        @Schema(description = "64-bit Steam ID", example = "76561198012345678")
        @NotBlank(message = "Steam ID is required")
        @Pattern(regexp = "^7656119\\d{10}$", message = "Invalid 64-bit SteamID format")
        String steamId,

        @Schema(description = "Display username", example = "dev_player")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Assigned user role (USER or ADMIN)", example = "USER")
        String role
) {
}
