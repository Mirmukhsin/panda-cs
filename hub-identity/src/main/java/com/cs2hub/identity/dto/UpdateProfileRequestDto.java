package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "User profile update request payload")
public record UpdateProfileRequestDto(
        @Schema(description = "Updated username (optional)", example = "dev1ce")
        @Size(min = 2, max = 128, message = "Username must be between 2 and 128 characters")
        String username,

        @Schema(description = "Steam trade offer URL (optional)", example = "https://steamcommunity.com/tradeoffer/new/?partner=12345678&token=abcdefgh")
        @Size(max = 255, message = "Trade URL cannot exceed 255 characters")
        String tradeUrl
) {
}
