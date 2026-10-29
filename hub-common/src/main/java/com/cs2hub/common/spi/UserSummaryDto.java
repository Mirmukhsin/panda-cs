package com.cs2hub.common.spi;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Public user summary projection for cross-module consumption")
public record UserSummaryDto(
        @Schema(description = "Unique user identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID userId,

        @Schema(description = "64-bit Steam ID", example = "76561198000000000")
        String steamId,

        @Schema(description = "Display / persona name", example = "s1mple")
        String username,

        @Schema(description = "Steam avatar full URL", example = "https://avatars.steamstatic.com/example.jpg")
        String avatarUrl
) implements Serializable {
}
