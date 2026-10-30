package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

@Schema(description = "Summary of a user's completed guess on a round")
public record CompletedGuessDto(
        @Schema(description = "Premier rating guessed by the user", example = "17500")
        int guessedPremierRating,

        @Schema(description = "Points awarded for the guess", example = "850")
        int pointsAwarded,

        @Schema(description = "Emoji representing the guess accuracy tier", example = "🟡")
        String tierEmoji,

        @Schema(description = "Absolute difference between actual and guessed rating", example = "950")
        int difference
) implements Serializable {
}
