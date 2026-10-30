package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

@Schema(description = "Summary report of catalog seeding or external synchronization")
public record SeedSummaryDto(
        @Schema(description = "Total number of weapon skins in catalog", example = "30")
        int skinsCount,

        @Schema(description = "Total number of stickers in catalog", example = "10")
        int stickersCount,

        @Schema(description = "Result summary message", example = "Successfully seeded curated CS2 skins and stickers")
        String message
) implements Serializable {
}
