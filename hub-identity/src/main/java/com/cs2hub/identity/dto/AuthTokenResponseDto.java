package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

@Schema(description = "Authentication response containing JWT access token and user profile")
public record AuthTokenResponseDto(
        @Schema(description = "JWT Bearer access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in milliseconds", example = "604800000")
        long expiresInMs,

        @Schema(description = "Authenticated user profile summary")
        UserProfileDto user
) implements Serializable {
    public static AuthTokenResponseDto bearer(String accessToken, long expiresInMs, UserProfileDto user) {
        return new AuthTokenResponseDto(accessToken, "Bearer", expiresInMs, user);
    }
}
