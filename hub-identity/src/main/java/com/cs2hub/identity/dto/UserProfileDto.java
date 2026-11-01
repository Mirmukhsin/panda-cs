package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed user profile response")
public record UserProfileDto(
        @Schema(description = "User unique identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "64-bit Steam ID", example = "76561198000000000")
        String steamId,

        @Schema(description = "User display name", example = "s1mple")
        String username,

        @Schema(description = "User email address", example = "player@cs2hub.com")
        String email,

        @Schema(description = "Steam avatar full URL", example = "https://avatars.steamstatic.com/example_full.jpg")
        String avatarUrl,

        @Schema(description = "Steam community profile URL", example = "https://steamcommunity.com/profiles/76561198000000000")
        String profileUrl,

        @Schema(description = "Steam trade URL for skin transfers and marketplace crafts", example = "https://steamcommunity.com/tradeoffer/new/?partner=12345678&token=abcdefgh")
        String tradeUrl,

        @Schema(description = "User system role", example = "USER")
        String role,

        @Schema(description = "Account registration timestamp")
        Instant createdAt
) implements Serializable {
    public UserProfileDto(UUID id, String steamId, String username, String avatarUrl, String profileUrl, String tradeUrl, String role, Instant createdAt) {
        this(id, steamId, username, null, avatarUrl, profileUrl, tradeUrl, role, createdAt);
    }
}
