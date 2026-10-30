package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Parsed match stats payload delivered by worker-demoparser")
public record ParsedMatchPayloadDto(
        @NotBlank
        @Schema(description = "Map identifier", example = "de_mirage")
        String mapName,

        @Schema(description = "Final score for Team A", example = "13")
        int teamAScore,

        @Schema(description = "Final score for Team B", example = "9")
        int teamBScore,

        @Schema(description = "Match length in seconds", example = "2410")
        int durationSeconds,

        @NotEmpty
        @Schema(description = "Complete player stat records (typically 10 players)")
        List<MatchPlayerStatsDto> players
) implements Serializable {}
