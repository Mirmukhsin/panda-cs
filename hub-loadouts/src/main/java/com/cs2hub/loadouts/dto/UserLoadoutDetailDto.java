package com.cs2hub.loadouts.dto;

import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.loadouts.domain.SkinColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full detail representation of a community loadout and weapon sticker crafts")
public record UserLoadoutDetailDto(
        @Schema(description = "Loadout unique identifier")
        UUID id,

        @Schema(description = "Creator user UUID")
        UUID userId,

        @Schema(description = "Loadout title", example = "Crimson Strike")
        String title,

        @Schema(description = "Theme color", example = "RED")
        SkinColor themeColor,

        @Schema(description = "Budget limit in USD", example = "100.00")
        BigDecimal budgetLimitUsd,

        @Schema(description = "Total cost of skins and stickers in USD", example = "92.50")
        BigDecimal totalCostUsd,

        @Schema(description = "Community upvotes count", example = "42")
        Integer upvotes,

        @Schema(description = "Whether loadout is publicly visible")
        Boolean isPublic,

        @Schema(description = "List of weapon items with skins and sticker craft configurations")
        List<UserLoadoutItemDetailDto> items,

        @Schema(description = "Author user profile information")
        UserSummaryDto author,

        @Schema(description = "Whether current requesting user has upvoted this loadout")
        Boolean upvotedByCurrentUser,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) implements Serializable {}
