package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Leaderboard ranking response")
public record LeaderboardResponseDto(
        @Schema(description = "Ranking time period (DAILY or ALL_TIME)", example = "DAILY")
        String period,

        @Schema(description = "Date for DAILY leaderboard, null for ALL_TIME", example = "2026-10-30")
        LocalDate date,

        @Schema(description = "Ranked list of top player entries")
        List<LeaderboardEntryDto> entries
) implements Serializable {
}
