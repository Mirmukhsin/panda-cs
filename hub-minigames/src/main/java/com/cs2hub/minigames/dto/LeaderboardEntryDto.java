package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Individual player leaderboard entry")
public record LeaderboardEntryDto(
        @Schema(description = "Leaderboard placement rank (1-indexed)", example = "1")
        int rank,

        @Schema(description = "User unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Player display name", example = "s1mple")
        String username,

        @Schema(description = "Player Steam avatar URL", example = "https://avatars.steamstatic.com/example.jpg")
        String avatarUrl,

        @Schema(description = "Total accumulated points", example = "2850")
        long score,

        @Schema(description = "Number of rounds completed", example = "3")
        long roundsPlayed
) implements Serializable {
}
