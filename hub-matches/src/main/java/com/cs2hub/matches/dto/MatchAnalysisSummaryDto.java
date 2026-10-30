package com.cs2hub.matches.dto;

import com.cs2hub.matches.domain.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact match analysis summary for listing feeds")
public record MatchAnalysisSummaryDto(
        @Schema(description = "Match UUID")
        UUID id,

        @Schema(description = "Match share code")
        String shareCode,

        @Schema(description = "Analysis status")
        MatchStatus status,

        @Schema(description = "Map played", example = "de_mirage")
        String mapName,

        @Schema(description = "Team A score", example = "13")
        int teamAScore,

        @Schema(description = "Team B score", example = "9")
        int teamBScore,

        @Schema(description = "Total rounds played", example = "22")
        int totalRounds,

        @Schema(description = "Match duration in seconds", example = "2410")
        int durationSeconds,

        @Schema(description = "Public 8-character share slug", example = "7K9X2B4F")
        String shareSlug,

        @Schema(description = "Submission timestamp")
        Instant createdAt,

        @Schema(description = "Completion timestamp")
        Instant completedAt
) implements Serializable {}
