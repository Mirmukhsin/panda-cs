package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Claimed match job for demo parsing worker")
public record ClaimMatchJobResponseDto(
        @Schema(description = "Match analysis UUID")
        UUID matchId,

        @Schema(description = "CS2 match share code")
        String shareCode,

        @Schema(description = "Demo file download URL")
        String demoUrl,

        @Schema(description = "Public share slug")
        String shareSlug
) implements Serializable {}
