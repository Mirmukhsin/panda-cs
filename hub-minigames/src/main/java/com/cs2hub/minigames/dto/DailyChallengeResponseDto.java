package com.cs2hub.minigames.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Daily 'Guess the Elo' challenge state and round clips")
public record DailyChallengeResponseDto(
        @Schema(description = "Daily challenge identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID challengeId,

        @Schema(description = "Challenge calendar date (UTC)", example = "2026-10-30")
        LocalDate challengeDate,

        @Schema(description = "3 gameplay clip rounds for today's challenge")
        List<RoundClipDto> rounds,

        @Schema(description = "Total score accumulated by user today", example = "2350")
        int totalScore,

        @Schema(description = "Whether the user/guest has completed all rounds for today", example = "false")
        boolean completed,

        @Schema(description = "Shareable Discord summary text once all 3 rounds are guessed, otherwise null")
        String shareText
) implements Serializable {
}
