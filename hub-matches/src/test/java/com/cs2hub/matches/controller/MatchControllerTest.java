package com.cs2hub.matches.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.matches.domain.MatchStatus;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.MatchAnalysisSummaryDto;
import com.cs2hub.matches.dto.PlayerMatchHistoryItemDto;
import com.cs2hub.matches.dto.PlayerRoastCardDto;
import com.cs2hub.matches.dto.SubmitMatchRequestDto;
import com.cs2hub.matches.service.MatchAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchAnalysisService matchAnalysisService;

    private MatchController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchController(matchAnalysisService);
    }

    @Test
    @DisplayName("POST /api/v1/matches should submit match and return 201 Created")
    void shouldSubmitMatch() {
        SubmitMatchRequestDto request = new SubmitMatchRequestDto("CSGO-SUBMIT-123", null);
        MatchAnalysisDetailDto detail = new MatchAnalysisDetailDto(
                UUID.randomUUID(), "CSGO-SUBMIT-123", null, null, MatchStatus.QUEUED,
                null, 0, 0, 0, 0, null, "SLUG1234",
                List.of(), List.of(), Instant.now(), null
        );

        when(matchAnalysisService.submitMatchForAnalysis(request, Optional.empty())).thenReturn(detail);

        ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> response = controller.submitMatch(Optional.empty(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareSlug()).isEqualTo("SLUG1234");
    }

    @Test
    @DisplayName("POST /api/v1/matches/simulate should return 201 Created with simulated match")
    void shouldSimulateMatch() {
        MatchAnalysisDetailDto detail = new MatchAnalysisDetailDto(
                UUID.randomUUID(), "CSGO-SIM-123", null, null, MatchStatus.COMPLETED,
                "de_mirage", 13, 11, 24, 2410, null, "SIM12345",
                List.of(), List.of(), Instant.now(), Instant.now()
        );

        when(matchAnalysisService.simulateMatchAnalysis(Optional.empty(), "de_mirage")).thenReturn(detail);

        ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> response =
                controller.simulateMatch(Optional.empty(), "de_mirage");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().totalRounds()).isEqualTo(24);
    }

    @Test
    @DisplayName("GET /api/v1/matches/recent should return page of recent matches")
    void shouldGetRecentMatches() {
        MatchAnalysisSummaryDto summary = new MatchAnalysisSummaryDto(
                UUID.randomUUID(), "CSGO-RECENT", MatchStatus.COMPLETED,
                "de_dust2", 13, 8, 21, 2100, "RECENT99", Instant.now(), Instant.now()
        );
        Page<MatchAnalysisSummaryDto> page = new PageImpl<>(List.of(summary));

        when(matchAnalysisService.getRecentMatches(0, 15)).thenReturn(page);

        ResponseEntity<ApiResponseDto<Page<MatchAnalysisSummaryDto>>> response =
                controller.getRecentMatches(0, 15);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/matches/player/{steamId} should return match history")
    void shouldGetPlayerMatchHistory() {
        String steamId = "76561198000000001";
        PlayerMatchHistoryItemDto item = new PlayerMatchHistoryItemDto(
                UUID.randomUUID(), "SLUG1234", "de_inferno", "13 - 9", true,
                22, 12, 88.5, 1.25, "Opening Bulldozer", "6 opening picks", List.of(), Instant.now()
        );

        when(matchAnalysisService.getPlayerMatchHistory(steamId)).thenReturn(List.of(item));

        ResponseEntity<ApiResponseDto<List<PlayerMatchHistoryItemDto>>> response =
                controller.getPlayerMatchHistory(steamId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/matches/{idOrSlug} should return match details")
    void shouldGetMatchByIdOrSlug() {
        String slug = "MATCH123";
        MatchAnalysisDetailDto detail = new MatchAnalysisDetailDto(
                UUID.randomUUID(), "CSGO-CODE", null, null, MatchStatus.COMPLETED,
                "de_anubis", 13, 10, 23, 2300, null, slug,
                List.of(), List.of(), Instant.now(), Instant.now()
        );

        when(matchAnalysisService.getMatchByIdOrSlug(slug)).thenReturn(detail);

        ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> response = controller.getMatchByIdOrSlug(slug);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareSlug()).isEqualTo(slug);
    }

    @Test
    @DisplayName("GET /api/v1/matches/{idOrSlug}/players/{steamId}/roast-card should return roast card")
    void shouldGetPlayerRoastCard() {
        String slug = "MATCH123";
        String steamId = "76561198000000001";
        PlayerRoastCardDto card = new PlayerRoastCardDto(
                UUID.randomUUID(), "s1mple", steamId, "de_mirage", "13 - 11", true,
                26, 12, 4, 102.5, 73.1, 1.48, "Headshot Demon", "73.1% HS accuracy",
                slug, "https://cs2hub.gg/card", List.of()
        );

        when(matchAnalysisService.getPlayerRoastCard(slug, steamId)).thenReturn(card);

        ResponseEntity<ApiResponseDto<PlayerRoastCardDto>> response =
                controller.getPlayerRoastCard(slug, steamId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().headlineTitle()).isEqualTo("Headshot Demon");
    }
}
