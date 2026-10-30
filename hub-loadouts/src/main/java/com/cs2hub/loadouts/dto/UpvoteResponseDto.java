package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Loadout upvote toggle response")
public record UpvoteResponseDto(
        @Schema(description = "Target loadout UUID")
        UUID loadoutId,

        @Schema(description = "Updated total upvotes count", example = "43")
        int upvotes,

        @Schema(description = "Whether the user now has an active upvote on this loadout", example = "true")
        boolean upvoted
) implements Serializable {}
