package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.minigames.dto.DailyChallengeResponseDto;
import com.cs2hub.minigames.dto.GuessResultResponseDto;
import com.cs2hub.minigames.dto.LeaderboardEntryDto;
import com.cs2hub.minigames.dto.LeaderboardResponseDto;
import com.cs2hub.minigames.dto.SubmitGuessRequestDto;
import com.cs2hub.minigames.service.DailyChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyChallengeControllerTest {

    @Mock
    private DailyChallengeService dailyChallengeService;

    private DailyChallengeController dailyChallengeController;

    @BeforeEach
    void setUp() {
        dailyChallengeController = new DailyChallengeController(dailyChallengeService);
    }

    @Test
    @DisplayName("GET /api/v1/minigames/daily should return 200 with DailyChallengeResponseDto")
    void shouldReturnDailyChallenge() {
        UUID challengeId = UUID.randomUUID();
        DailyChallengeResponseDto mockResponse = new DailyChallengeResponseDto(
                challengeId, LocalDate.of(2026, 10, 30), Collections.emptyList(), 0, false, null
        );

        when(dailyChallengeService.getTodayChallenge(any(), eq("guest_123"))).thenReturn(mockResponse);

        ResponseEntity<ApiResponseDto<DailyChallengeResponseDto>> response =
                dailyChallengeController.getTodayChallenge(Optional.empty(), "guest_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().challengeId()).isEqualTo(challengeId);
    }

    @Test
    @DisplayName("POST /api/v1/minigames/daily/guess should return 200 with GuessResultResponseDto")
    void shouldSubmitGuess() {
        UUID clipId = UUID.randomUUID();
        SubmitGuessRequestDto request = new SubmitGuessRequestDto(clipId, 16000);

        GuessResultResponseDto mockResult = new GuessResultResponseDto(
                clipId, 1, 16500, 7, 16000, 500, 1000, "🟢", 1000, false, null
        );

        when(dailyChallengeService.submitGuess(any(), eq("guest_123"), eq(request))).thenReturn(mockResult);

        ResponseEntity<ApiResponseDto<GuessResultResponseDto>> response =
                dailyChallengeController.submitGuess(Optional.empty(), "guest_123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().pointsAwarded()).isEqualTo(1000);
        assertThat(response.getBody().data().tierEmoji()).isEqualTo("🟢");
    }

    @Test
    @DisplayName("GET /api/v1/minigames/leaderboard should return 200 with LeaderboardResponseDto")
    void shouldReturnLeaderboard() {
        LeaderboardEntryDto entry = new LeaderboardEntryDto(
                1, UUID.randomUUID(), "s1mple", "https://avatar/s1mple.jpg", 3000L, 3L
        );
        LeaderboardResponseDto mockResponse = new LeaderboardResponseDto("DAILY", LocalDate.of(2026, 10, 30), List.of(entry));

        when(dailyChallengeService.getLeaderboard("DAILY", LocalDate.of(2026, 10, 30), 20))
                .thenReturn(mockResponse);

        ResponseEntity<ApiResponseDto<LeaderboardResponseDto>> response =
                dailyChallengeController.getLeaderboard("DAILY", LocalDate.of(2026, 10, 30), 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().entries()).hasSize(1);
        assertThat(response.getBody().data().entries().get(0).username()).isEqualTo("s1mple");
    }
}
