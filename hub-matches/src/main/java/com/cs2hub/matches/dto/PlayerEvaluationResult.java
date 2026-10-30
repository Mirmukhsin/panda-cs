package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Evaluation output containing headline title, roast, and awarded badges")
public record PlayerEvaluationResult(
        @Schema(description = "Primary headline badge title", example = "Flashbang Philanthropist")
        String headlineTitle,

        @Schema(description = "Summary 1-sentence roast or flex for card generation", example = "Blinded teammates 14 times. Enemy team owes you a sponsorship.")
        String headlineRoast,

        @Schema(description = "All earned badges meeting statistical triggers")
        List<AwardBadgeDto> badges
) implements Serializable {}
