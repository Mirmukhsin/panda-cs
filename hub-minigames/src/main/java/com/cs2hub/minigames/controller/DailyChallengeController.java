package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.minigames.dto.DailyChallengeResponseDto;
import com.cs2hub.minigames.dto.GuessResultResponseDto;
import com.cs2hub.minigames.dto.LeaderboardResponseDto;
import com.cs2hub.minigames.dto.SubmitGuessRequestDto;
import com.cs2hub.minigames.service.DailyChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@Tag(name = "Minigames Daily Challenge", description = "Endpoints for playing daily 'Guess the Elo' challenge, submitting guesses, and viewing leaderboards")
@RestController
@RequestMapping("/api/v1/minigames")
@RequiredArgsConstructor
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;

    @Operation(
            summary = "Get Today's Daily 'Guess the Elo' Challenge",
            description = "Returns today's 3 gameplay clips. Anti-cheat protection masks actual ratings until a guess is submitted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Daily challenge state retrieved",
                    content = @Content(schema = @Schema(implementation = DailyChallengeResponseDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "No active daily challenge available for today")
    })
    @GetMapping("/daily")
    public ResponseEntity<ApiResponseDto<DailyChallengeResponseDto>> getTodayChallenge(
            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser,
            @Parameter(description = "Optional anonymous guest session ID header for zero-friction play")
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId
    ) {
        DailyChallengeResponseDto challenge = dailyChallengeService.getTodayChallenge(currentUser, guestSessionId);
        return ResponseEntity.ok(ApiResponseDto.ok(challenge));
    }

    @Operation(
            summary = "Submit a Guess for a Round Clip",
            description = "Submits player rating estimate for a round. Immediately calculates score, points awarded, tier emoji, and reveals actual ratings."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Guess evaluated and scored",
                    content = @Content(schema = @Schema(implementation = GuessResultResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate guess for clip"),
            @ApiResponse(responseCode = "404", description = "Daily challenge or clip not found")
    })
    @PostMapping("/daily/guess")
    public ResponseEntity<ApiResponseDto<GuessResultResponseDto>> submitGuess(
            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser,
            @Parameter(description = "Optional anonymous guest session ID header")
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId,
            @Valid @RequestBody SubmitGuessRequestDto request
    ) {
        GuessResultResponseDto result = dailyChallengeService.submitGuess(currentUser, guestSessionId, request);
        return ResponseEntity.ok(ApiResponseDto.ok("Guess submitted and scored", result));
    }

    @Operation(
            summary = "Get 'Guess the Elo' Leaderboard",
            description = "Retrieves ranked player scores for today's challenge or all-time aggregated rankings."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Leaderboard rankings retrieved",
                    content = @Content(schema = @Schema(implementation = LeaderboardResponseDto.class))
            )
    })
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponseDto<LeaderboardResponseDto>> getLeaderboard(
            @Parameter(description = "Time period filter: DAILY or ALL_TIME (default: DAILY)")
            @RequestParam(required = false, defaultValue = "DAILY") String period,
            @Parameter(description = "Specific calendar date for DAILY leaderboard (defaults to today UTC)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Max number of entries to return (default: 20, max: 100)")
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        LeaderboardResponseDto leaderboard = dailyChallengeService.getLeaderboard(period, date, limit);
        return ResponseEntity.ok(ApiResponseDto.ok(leaderboard));
    }
}
