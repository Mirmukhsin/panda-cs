package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Aggregated CS2 Events, Giveaways and Deals Hub payload")
public record EventsHubResponseDto(
        @Schema(description = "List of active skin and wallet giveaways")
        List<GiveawayDto> giveaways,

        @Schema(description = "List of curated marketplace discount deals")
        List<DiscountDealDto> discounts,

        @Schema(description = "List of current and upcoming CS2 tournaments and Valve special events")
        List<SpecialEventDto> specialEvents
) implements Serializable {
}
