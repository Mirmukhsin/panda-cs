package com.cs2hub.minigames.dto;

import com.cs2hub.minigames.domain.ClipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Admin review decision for a submitted clip")
public record ReviewClipRequestDto(
        @Schema(description = "Review decision: APPROVED or REJECTED", example = "APPROVED")
        @NotNull(message = "Review status is required")
        ClipStatus status,

        @Schema(description = "Optional override for Premier Rating if verified differently", example = "17000")
        @Min(value = 1000, message = "Premier rating must be at least 1000")
        @Max(value = 35000, message = "Premier rating cannot exceed 35000")
        Integer overridePremierRating
) {
}
