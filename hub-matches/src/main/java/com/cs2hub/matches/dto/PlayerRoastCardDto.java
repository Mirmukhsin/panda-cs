package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Schema(description = "Focused data payload optimized for generating shareable 1200x630 Discord/Twitter roast cards")
public record PlayerRoastCardDto(
        @Schema(description = "Match analysis UUID")
        UUID matchId,

        @Schema(description = "Player persona name", example = "s1mple")
        String playerName,

        @Schema(description = "64-bit Steam ID", example = "76561198000000001")
        String steamId,

        @Schema(description = "Map played", example = "de_mirage")
        String mapName,

        @Schema(description = "Score line", example = "13 - 11")
        String scoreLine,

        @Schema(description = "Whether the player's team won the match")
        boolean won,

        @Schema(description = "Kills")
        int kills,

        @Schema(description = "Deaths")
        int deaths,

        @Schema(description = "Assists")
        int assists,

        @Schema(description = "Average Damage per Round")
        double adr,

        @Schema(description = "Headshot percentage")
        double headshotPct,

        @Schema(description = "HLTV 2.0 Impact Rating")
        double impactRating,

        @Schema(description = "Headline title badge", example = "Flashbang Philanthropist")
        String headlineTitle,

        @Schema(description = "Headline roast statement", example = "Blinded teammates 14 times. Enemy team owes you a sponsorship.")
        String headlineRoast,

        @Schema(description = "8-character share slug", example = "7K9X2B4F")
        String shareSlug,

        @Schema(description = "Shareable web link", example = "https://cs2hub.gg/matches/7K9X2B4F/players/76561198000000001")
        String shareUrl,

        @Schema(description = "Badges earned in this match")
        List<AwardBadgeDto> badges
) implements Serializable {}
