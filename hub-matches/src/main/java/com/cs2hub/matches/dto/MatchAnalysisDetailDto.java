package com.cs2hub.matches.dto;

import com.cs2hub.matches.domain.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Complete match analysis report including full player scoreboards and roast badges")
public record MatchAnalysisDetailDto(
        @Schema(description = "Match UUID")
        UUID id,

        @Schema(description = "Match share code")
        String shareCode,

        @Schema(description = "Match demo URL")
        String demoUrl,

        @Schema(description = "Submitter user UUID if authenticated")
        UUID submittedByUserId,

        @Schema(description = "Analysis status (QUEUED, PARSING, COMPLETED, FAILED)")
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

        @Schema(description = "Error message if status is FAILED")
        String errorMessage,

        @Schema(description = "Public 8-character share slug", example = "7K9X2B4F")
        String shareSlug,

        @Schema(description = "Players on Team A")
        List<MatchPlayerDetailDto> teamAPlayers,

        @Schema(description = "Players on Team B")
        List<MatchPlayerDetailDto> teamBPlayers,

        @Schema(description = "Analysis submission timestamp")
        Instant createdAt,

        @Schema(description = "Analysis completion timestamp")
        Instant completedAt
) implements Serializable {}
