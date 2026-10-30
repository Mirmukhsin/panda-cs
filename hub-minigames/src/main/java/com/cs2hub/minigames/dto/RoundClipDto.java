package com.cs2hub.minigames.dto;

import com.cs2hub.minigames.domain.VideoProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Gameplay clip assigned to a specific round in the daily challenge")
public record RoundClipDto(
        @Schema(description = "Round order (1, 2, or 3)", example = "1")
        int roundOrder,

        @Schema(description = "Clip unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID clipId,

        @Schema(description = "Clip title", example = "Mirage A site 1v3 clutch")
        String title,

        @Schema(description = "Video stream or embed URL", example = "https://www.youtube.com/watch?v=sample123")
        String videoUrl,

        @Schema(description = "Video hosting provider", example = "YOUTUBE")
        VideoProvider videoProvider,

        @Schema(description = "Map played in clip", example = "de_mirage")
        String mapName,

        @Schema(description = "Actual Premier rating (Anti-Cheat: NULL until guessed)", example = "16500")
        Integer actualPremierRating,

        @Schema(description = "Actual FACEIT level (Anti-Cheat: NULL until guessed)", example = "8")
        Integer actualFaceitLevel,

        @Schema(description = "Completed guess details if already guessed by player, else null")
        CompletedGuessDto userGuess
) implements Serializable {
}
