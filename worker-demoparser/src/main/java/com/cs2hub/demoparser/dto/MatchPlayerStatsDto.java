package com.cs2hub.demoparser.dto;

import java.io.Serializable;

public record MatchPlayerStatsDto(
        String steamId,
        String playerName,
        String teamSide,
        int kills,
        int deaths,
        int assists,
        double adr,
        double headshotPct,
        double kastPct,
        double impactRating,
        int firstKills,
        int firstDeaths,
        int clutchesWon,
        int teamFlashes,
        int enemiesFlashed,
        int utilityDamage,
        int zeroDamageRounds,
        int ecoKills,
        int awpKills,
        int bombPlants,
        int bombDefuses
) implements Serializable {}
