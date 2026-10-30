package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload to submit a player guess for a round clip")
public record SubmitGuessRequestDto(
        @Schema(description = "UUID of the clip being guessed", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Clip ID is required")
        UUID clipId,

        @Schema(description = "Estimated CS2 Premier rating (1000 to 35000)", example = "17200")
        @NotNull(message = "Guessed premier rating is required")
        @Min(value = 1000, message = "Premier rating must be at least 1000")
        @Max(value = 35000, message = "Premier rating cannot exceed 35000")
        Integer guessedPremierRating
) {
}
