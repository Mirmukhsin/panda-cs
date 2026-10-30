package com.cs2hub.matches.dto;

import com.cs2hub.matches.domain.MatchTeamSide;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed match scoreboard performance and awards for an individual player")
public record MatchPlayerDetailDto(
        @Schema(description = "Match player entry UUID")
        UUID id,

        @Schema(description = "64-bit Steam ID", example = "76561198000000001")
        String steamId,

        @Schema(description = "User account UUID if registered on platform")
        UUID userId,

        @Schema(description = "Player persona name", example = "s1mple")
        String playerName,

        @Schema(description = "Team side in match", example = "TEAM_A")
        MatchTeamSide teamSide,

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

        @Schema(description = "Kill, Assist, Survived, Traded percentage")
        double kastPct,

        @Schema(description = "HLTV 2.0 Impact Rating")
        double impactRating,

        @Schema(description = "Opening duel kills")
        int firstKills,

        @Schema(description = "Opening duel deaths")
        int firstDeaths,

        @Schema(description = "1vX clutch rounds won")
        int clutchesWon,

        @Schema(description = "Teammates blinded by player's flashbangs")
        int teamFlashes,

        @Schema(description = "Enemies blinded by player's flashbangs")
        int enemiesFlashed,

        @Schema(description = "Total utility grenade damage")
        int utilityDamage,

        @Schema(description = "Rounds where player dealt exactly 0 damage")
        int zeroDamageRounds,

        @Schema(description = "Eco/anti-eco round kills")
        int ecoKills,

        @Schema(description = "AWP sniper kills")
        int awpKills,

        @Schema(description = "C4 plants")
        int bombPlants,

        @Schema(description = "C4 defuses")
        int bombDefuses,

        @Schema(description = "Primary assigned headline badge title", example = "Flashbang Philanthropist")
        String headlineTitle,

        @Schema(description = "Assigned headline roast or flex description")
        String headlineRoast,

        @Schema(description = "All qualifying awards earned in this match")
        List<AwardBadgeDto> awards
) implements Serializable {}
