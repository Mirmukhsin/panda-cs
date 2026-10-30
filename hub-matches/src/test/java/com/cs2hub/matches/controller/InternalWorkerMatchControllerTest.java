package com.cs2hub.matches.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.matches.domain.MatchStatus;
import com.cs2hub.matches.dto.ClaimMatchJobResponseDto;
import com.cs2hub.matches.dto.FailMatchJobRequestDto;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.ParsedMatchPayloadDto;
import com.cs2hub.matches.service.MatchAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalWorkerMatchControllerTest {

    @Mock
    private MatchAnalysisService matchAnalysisService;

    private InternalWorkerMatchController controller;

    private static final String VALID_SECRET = "valid-secret-key";

    @BeforeEach
    void setUp() {
        controller = new InternalWorkerMatchController(matchAnalysisService);
    }

    @Test
    @DisplayName("claimNextJob should validate secret and return claimed job")
    void shouldClaimNextJob() {
        ClaimMatchJobResponseDto job = new ClaimMatchJobResponseDto(
                UUID.randomUUID(), "CSGO-CLAIM", "https://demo.url", "CLAIM123"
        );

        when(matchAnalysisService.claimNextQueuedJob()).thenReturn(Optional.of(job));

        ResponseEntity<ApiResponseDto<ClaimMatchJobResponseDto>> response =
                controller.claimNextJob(VALID_SECRET);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareSlug()).isEqualTo("CLAIM123");
        verify(matchAnalysisService).validateWorkerSecret(VALID_SECRET);
    }

    @Test
    @DisplayName("completeMatch should validate secret and complete match")
    void shouldCompleteMatch() {
        UUID matchId = UUID.randomUUID();
        ParsedMatchPayloadDto payload = new ParsedMatchPayloadDto(
                "de_mirage", 13, 11, 2400, List.of()
        );

        MatchAnalysisDetailDto detail = new MatchAnalysisDetailDto(
                matchId, "CSGO-CODE", null, null, MatchStatus.COMPLETED,
                "de_mirage", 13, 11, 24, 2400, null, "SLUG1234",
                List.of(), List.of(), Instant.now(), Instant.now()
        );

        when(matchAnalysisService.completeMatchAnalysis(matchId, payload)).thenReturn(detail);

        ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> response =
                controller.completeMatch(VALID_SECRET, matchId, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo(MatchStatus.COMPLETED);
        verify(matchAnalysisService).validateWorkerSecret(VALID_SECRET);
    }

    @Test
    @DisplayName("failMatch should validate secret and fail match")
    void shouldFailMatch() {
        UUID matchId = UUID.randomUUID();
        FailMatchJobRequestDto request = new FailMatchJobRequestDto("Corrupt demo file");

        MatchAnalysisDetailDto detail = new MatchAnalysisDetailDto(
                matchId, "CSGO-CODE", null, null, MatchStatus.FAILED,
                null, 0, 0, 0, 0, "Corrupt demo file", "SLUG1234",
                List.of(), List.of(), Instant.now(), Instant.now()
        );

        when(matchAnalysisService.failMatchAnalysis(matchId, request)).thenReturn(detail);

        ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> response =
                controller.failMatch(VALID_SECRET, matchId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo(MatchStatus.FAILED);
        verify(matchAnalysisService).validateWorkerSecret(VALID_SECRET);
    }

    @Test
    @DisplayName("Endpoints should throw UnauthorizedException on invalid worker secret")
    void shouldRejectInvalidSecret() {
        doThrow(new UnauthorizedException("Invalid secret"))
                .when(matchAnalysisService).validateWorkerSecret("bad-secret");

        assertThatThrownBy(() -> controller.claimNextJob("bad-secret"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
