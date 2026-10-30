package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Player match history record with performance stats and earned roast badges")
public record PlayerMatchHistoryItemDto(
        @Schema(description = "Match UUID")
        UUID matchId,

        @Schema(description = "Public 8-character share slug", example = "7K9X2B4F")
        String shareSlug,

        @Schema(description = "Map played", example = "de_mirage")
        String mapName,

        @Schema(description = "Score line", example = "13 - 11")
        String scoreLine,

        @Schema(description = "Whether player won")
        boolean won,

        @Schema(description = "Kills")
        int kills,

        @Schema(description = "Deaths")
        int deaths,

        @Schema(description = "ADR")
        double adr,

        @Schema(description = "Impact rating")
        double impactRating,

        @Schema(description = "Primary headline badge title")
        String headlineTitle,

        @Schema(description = "Primary headline roast statement")
        String headlineRoast,

        @Schema(description = "Badges earned in this match")
        List<AwardBadgeDto> badges,

        @Schema(description = "Match date")
        Instant matchDate
) implements Serializable {}
