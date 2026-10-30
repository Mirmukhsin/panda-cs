package com.cs2hub.matches.dto;

import com.cs2hub.matches.domain.MatchTeamSide;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Individual player performance statistics within a match")
public record MatchPlayerStatsDto(
        @Schema(description = "64-bit Steam ID", example = "76561198000000001")
        String steamId,

        @Schema(description = "In-game player persona name", example = "s1mple")
        String playerName,

        @Schema(description = "Team side in match", example = "TEAM_A")
        MatchTeamSide teamSide,

        @Schema(description = "Total kills", example = "24")
        int kills,

        @Schema(description = "Total deaths", example = "12")
        int deaths,

        @Schema(description = "Total assists", example = "5")
        int assists,

        @Schema(description = "Average Damage per Round", example = "89.5")
        double adr,

        @Schema(description = "Headshot percentage", example = "54.2")
        double headshotPct,

        @Schema(description = "Kill, Assist, Survived, Traded percentage", example = "73.5")
        double kastPct,

        @Schema(description = "HLTV 2.0 Impact Rating", example = "1.35")
        double impactRating,

        @Schema(description = "Opening duel kills", example = "4")
        int firstKills,

        @Schema(description = "Opening duel deaths", example = "2")
        int firstDeaths,

        @Schema(description = "1vX clutch rounds won", example = "2")
        int clutchesWon,

        @Schema(description = "Times teammates were blinded by this player's flashes", example = "1")
        int teamFlashes,

        @Schema(description = "Times enemy players were blinded", example = "18")
        int enemiesFlashed,

        @Schema(description = "Total utility grenade damage dealt", example = "180")
        int utilityDamage,

        @Schema(description = "Number of rounds where player dealt exactly 0 damage", example = "2")
        int zeroDamageRounds,

        @Schema(description = "Kills against unarmored or eco opponents", example = "3")
        int ecoKills,

        @Schema(description = "Kills secured with the AWP sniper rifle", example = "14")
        int awpKills,

        @Schema(description = "Number of C4 bomb plants", example = "2")
        int bombPlants,

        @Schema(description = "Number of C4 bomb defuses", example = "1")
        int bombDefuses
) implements Serializable {}
