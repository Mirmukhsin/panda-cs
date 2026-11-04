package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;

@Schema(description = "CS2 Major tournament, Valve operation or in-game drop event")
public record SpecialEventDto(
        @Schema(description = "Unique event ID", example = "event-shanghai-major-2024")
        String id,

        @Schema(description = "Event title", example = "Perfect World Shanghai Major 2024")
        String title,

        @Schema(description = "Event classification (e.g. MAJOR, VALVE_DROP, TOURNAMENT)", example = "MAJOR")
        String eventType,

        @Schema(description = "Event summary and highlights")
        String description,

        @Schema(description = "Event starting timestamp")
        Instant startDate,

        @Schema(description = "Event ending timestamp")
        Instant endDate,

        @Schema(description = "Prize pool or event rewards", example = "$1,250,000 USD")
        String prizePool,

        @Schema(description = "Host city/venue location", example = "Shanghai, China")
        String location,

        @Schema(description = "Banner image URL")
        String bannerUrl,

        @Schema(description = "Official tournament or update URL")
        String actionUrl
) implements Serializable {
}
