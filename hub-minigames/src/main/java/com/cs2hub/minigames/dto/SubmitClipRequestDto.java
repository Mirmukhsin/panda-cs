package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to submit a CS2 gameplay clip for Guess the Elo minigame")
public record SubmitClipRequestDto(
        @Schema(description = "Clip title or description", example = "Clean Mirage A execute 1v3 clutch")
        @NotBlank(message = "Title is required")
        @Size(max = 160, message = "Title cannot exceed 160 characters")
        String title,

        @Schema(description = "Link to the gameplay video (YouTube, Medal.tv, Twitch, or MP4 URL)", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        @NotBlank(message = "Video URL is required")
        String videoUrl,

        @Schema(description = "Competitive map name", example = "de_mirage")
        @NotBlank(message = "Map name is required")
        String mapName,

        @Schema(description = "Actual CS2 Premier Rating of the player in the clip (1000 - 35000)", example = "16500")
        @NotNull(message = "Premier rating is required")
        @Min(value = 1000, message = "Premier rating must be at least 1000")
        @Max(value = 35000, message = "Premier rating cannot exceed 35000")
        Integer actualPremierRating,

        @Schema(description = "Optional FACEIT level (1 - 10) if applicable", example = "7")
        @Min(value = 1, message = "FACEIT level must be between 1 and 10")
        @Max(value = 10, message = "FACEIT level must be between 1 and 10")
        Integer actualFaceitLevel
) {
}
