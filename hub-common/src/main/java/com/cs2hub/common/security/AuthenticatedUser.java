package com.cs2hub.common.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Authenticated user context payload")
public record AuthenticatedUser(
        @Schema(description = "Unique user identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID userId,

        @Schema(description = "64-bit Steam ID", example = "76561198000000000")
        String steamId,

        @Schema(description = "User display name / persona name", example = "s1mple")
        String username,

        @Schema(description = "User authorization role", example = "USER")
        String role
) implements Serializable {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }
}
