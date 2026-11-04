package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Active community or sponsored CS2 item giveaway")
public record GiveawayDto(
        @Schema(description = "Unique giveaway ID", example = "giveaway-ak47-bloodsport")
        String id,

        @Schema(description = "Giveaway item title", example = "AK-47 | Bloodsport (Factory New)")
        String title,

        @Schema(description = "Estimated market value in USD", example = "145.00")
        BigDecimal valueUsd,

        @Schema(description = "Item rarity grade", example = "Covert")
        String rarity,

        @Schema(description = "Item preview image URL")
        String imageUrl,

        @Schema(description = "Giveaway conclusion timestamp")
        Instant endsAt,

        @Schema(description = "Total number of recorded participant entries", example = "1240")
        int totalEntries,

        @Schema(description = "Whether the current requesting user or guest has entered", example = "false")
        boolean entered
) implements Serializable {
}
