package com.cs2hub.demoparser.dto;

import java.io.Serializable;
import java.util.UUID;

public record ClaimMatchJobDto(
        UUID matchId,
        String shareCode,
        String demoUrl,
        String shareSlug
) implements Serializable {}
