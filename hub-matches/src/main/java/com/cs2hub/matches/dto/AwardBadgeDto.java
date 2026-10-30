package com.cs2hub.matches.dto;

import com.cs2hub.matches.domain.BadgeCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Roast or Flex badge earned by a player in a match")
public record AwardBadgeDto(
        @Schema(description = "Unique badge identifier code", example = "FLASHBANG_PHILANTHROPIST")
        String badgeCode,

        @Schema(description = "Display title of the badge", example = "Flashbang Philanthropist")
        String badgeTitle,

        @Schema(description = "Badge category (ROAST, FLEX, NEUTRAL)")
        BadgeCategory badgeCategory,

        @Schema(description = "Emoji or icon key for badge visual", example = "🕶️")
        String badgeIcon,

        @Schema(description = "Humorous stat-backed description or roast", example = "Blinded teammates 14 times. Enemy team owes you a sponsorship.")
        String description
) implements Serializable {}
