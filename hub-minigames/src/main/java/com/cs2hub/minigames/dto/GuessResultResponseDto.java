package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Result returned immediately after submitting an Elo guess")
public record GuessResultResponseDto(
        @Schema(description = "Clip identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID clipId,

        @Schema(description = "Round number (1, 2, or 3)", example = "1")
        int roundOrder,

        @Schema(description = "Revealed actual CS2 Premier rating", example = "16500")
        int actualPremierRating,

        @Schema(description = "Revealed actual FACEIT level if available", example = "8")
        Integer actualFaceitLevel,

        @Schema(description = "Player guessed Premier rating", example = "17200")
        int guessedPremierRating,

        @Schema(description = "Absolute difference between actual and guessed rating", example = "700")
        int difference,

        @Schema(description = "Points awarded for this round (0 - 1000)", example = "933")
        int pointsAwarded,

        @Schema(description = "Accuracy tier emoji (🟢 Bullseye, 🟡 Close, 🟠 Grazed, 🔴 Miss)", example = "🟡")
        String tierEmoji,

        @Schema(description = "Accumulated total score across completed rounds today", example = "1933")
        int totalChallengeScore,

        @Schema(description = "Whether all 3 daily rounds are now finished", example = "false")
        boolean completed,

        @Schema(description = "Discord-formatted summary string when all 3 rounds are finished, else null")
        String shareText
) implements Serializable {
}
