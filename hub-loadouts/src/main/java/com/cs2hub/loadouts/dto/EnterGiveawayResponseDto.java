package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Giveaway entry response")
public record EnterGiveawayResponseDto(
        @Schema(description = "Giveaway ID entered", example = "giveaway-ak47-bloodsport")
        String giveawayId,

        @Schema(description = "Updated total participant count", example = "1241")
        int totalEntries,

        @Schema(description = "Whether the user or guest has entered", example = "true")
        boolean entered,

        @Schema(description = "Status or confirmation message")
        String message
) implements Serializable {
}
