package com.cs2hub.demoparser.dto;

import java.io.Serializable;
import java.util.List;

public record ParsedMatchPayloadDto(
        String mapName,
        int teamAScore,
        int teamBScore,
        int durationSeconds,
        List<MatchPlayerStatsDto> players
) implements Serializable {}
