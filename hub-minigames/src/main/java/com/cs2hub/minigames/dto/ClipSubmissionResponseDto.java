package com.cs2hub.minigames.dto;

import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.VideoProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Details of a submitted clip")
public record ClipSubmissionResponseDto(
        @Schema(description = "Clip unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Clip title", example = "Clean Mirage A execute 1v3 clutch")
        String title,

        @Schema(description = "Video streaming / playback URL", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        String videoUrl,

        @Schema(description = "Detected video streaming provider", example = "YOUTUBE")
        VideoProvider videoProvider,

        @Schema(description = "Map played in clip", example = "de_mirage")
        String mapName,

        @Schema(description = "Actual CS2 Premier rating", example = "16500")
        Integer actualPremierRating,

        @Schema(description = "Actual FACEIT level if provided", example = "7")
        Integer actualFaceitLevel,

        @Schema(description = "Current moderation status", example = "PENDING")
        ClipStatus status,

        @Schema(description = "Profile summary of the submitter")
        UserSummaryDto submittedBy,

        @Schema(description = "UUID of the administrator who reviewed this clip, if reviewed")
        UUID reviewedByUserId,

        @Schema(description = "Timestamp when the clip was submitted")
        Instant createdAt,

        @Schema(description = "Timestamp when the clip was last updated")
        Instant updatedAt
) implements Serializable {
}
