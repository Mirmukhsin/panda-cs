package com.cs2hub.matches.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.matches.domain.BadgeCategory;
import com.cs2hub.matches.domain.MatchAnalysisEntity;
import com.cs2hub.matches.domain.MatchPlayerEntity;
import com.cs2hub.matches.domain.MatchStatus;
import com.cs2hub.matches.domain.MatchTeamSide;
import com.cs2hub.matches.dto.AwardBadgeDto;
import com.cs2hub.matches.dto.ClaimMatchJobResponseDto;
import com.cs2hub.matches.dto.FailMatchJobRequestDto;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.MatchPlayerStatsDto;
import com.cs2hub.matches.dto.ParsedMatchPayloadDto;
import com.cs2hub.matches.dto.PlayerEvaluationResult;
import com.cs2hub.matches.dto.PlayerMatchHistoryItemDto;
import com.cs2hub.matches.dto.PlayerRoastCardDto;
import com.cs2hub.matches.dto.SubmitMatchRequestDto;
import com.cs2hub.matches.repository.MatchAnalysisRepository;
import com.cs2hub.matches.repository.MatchPlayerRepository;
import com.cs2hub.matches.repository.PlayerAwardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchAnalysisServiceTest {

    @Mock
    private MatchAnalysisRepository matchAnalysisRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PlayerAwardRepository playerAwardRepository;

    @Mock
    private RoastBadgeEngine roastBadgeEngine;

    @Mock
    private UserLookupService userLookupService;

    private MatchAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new MatchAnalysisService(
                matchAnalysisRepository,
                matchPlayerRepository,
                playerAwardRepository,
                roastBadgeEngine,
                userLookupService
        );
        ReflectionTestUtils.setField(service, "workerSecret", "test-secret-key");
    }

    @Test
    @DisplayName("validateWorkerSecret should verify secret or throw UnauthorizedException")
    void shouldValidateWorkerSecret() {
        service.validateWorkerSecret("test-secret-key");

        assertThatThrownBy(() -> service.validateWorkerSecret("wrong-secret"))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> service.validateWorkerSecret(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("submitMatchForAnalysis should complete match deterministically or return existing if already completed")
    void shouldSubmitMatchIdempotently() {
        SubmitMatchRequestDto request = new SubmitMatchRequestDto("CSGO-EXISTING-CODE", null);

        MatchPlayerEntity samplePlayer = MatchPlayerEntity.builder()
                .id(UUID.randomUUID())
                .steamId("76561198000000001")
                .playerName("s1mple")
                .teamSide(MatchTeamSide.TEAM_A)
                .kills(20)
                .deaths(10)
                .adr(90.0)
                .impactRating(1.2)
                .awards(new ArrayList<>())
                .build();

        MatchAnalysisEntity existing = MatchAnalysisEntity.builder()
                .id(UUID.randomUUID())
                .shareCode("CSGO-EXISTING-CODE")
                .status(MatchStatus.COMPLETED)
                .shareSlug("EXIST123")
                .players(new ArrayList<>(List.of(samplePlayer)))
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.findByShareCode("CSGO-EXISTING-CODE")).thenReturn(Optional.of(existing));

        MatchAnalysisDetailDto result = service.submitMatchForAnalysis(request, Optional.empty());
        assertThat(result.shareSlug()).isEqualTo("EXIST123");
        assertThat(result.status()).isEqualTo(MatchStatus.COMPLETED);

        // New submission - auto transitions to COMPLETED via deterministic fallback
        SubmitMatchRequestDto newReq = new SubmitMatchRequestDto("CSGO-NEW-CODE", null);
        when(matchAnalysisRepository.findByShareCode("CSGO-NEW-CODE")).thenReturn(Optional.empty());
        when(matchAnalysisRepository.findByShareSlug(any())).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> {
            MatchAnalysisEntity e = i.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            if (e.getPlayers() == null) e.setPlayers(new ArrayList<>());
            return e;
        });

        AwardBadgeDto fallbackBadge = new AwardBadgeDto(
                "SOLID_SOLDIER", "Solid Soldier", BadgeCategory.FLEX, "🎖️", "Solid play"
        );
        when(roastBadgeEngine.evaluatePlayer(any(), anyInt())).thenReturn(
                new PlayerEvaluationResult("Solid Soldier", "Clocked in, clocked out.", List.of(fallbackBadge))
        );

        MatchAnalysisDetailDto newResult = service.submitMatchForAnalysis(newReq, Optional.empty());
        assertThat(newResult.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(newResult.shareSlug()).hasSize(8);
        assertThat(newResult.teamAPlayers()).hasSize(5);
        assertThat(newResult.teamBPlayers()).hasSize(5);
        assertThat(newResult.totalRounds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("submitMatchForAnalysis with demoUrl should keep match QUEUED for worker demo parser")
    void shouldQueueMatchWhenDemoUrlProvided() {
        SubmitMatchRequestDto demoReq = new SubmitMatchRequestDto("CSGO-DEMO-CODE", "https://demos.cs2hub.com/match.dem");
        when(matchAnalysisRepository.findByShareCode("CSGO-DEMO-CODE")).thenReturn(Optional.empty());
        when(matchAnalysisRepository.findByShareSlug(any())).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> {
            MatchAnalysisEntity e = i.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            if (e.getPlayers() == null) e.setPlayers(new ArrayList<>());
            return e;
        });

        MatchAnalysisDetailDto result = service.submitMatchForAnalysis(demoReq, Optional.empty());
        assertThat(result.status()).isEqualTo(MatchStatus.QUEUED);
        assertThat(result.demoUrl()).isEqualTo("https://demos.cs2hub.com/match.dem");
    }

    @Test
    @DisplayName("getMatchByIdOrSlug should auto-parse QUEUED match to COMPLETED")
    void shouldAutoParseQueuedMatchOnGet() {
        UUID matchId = UUID.randomUUID();
        MatchAnalysisEntity queued = MatchAnalysisEntity.builder()
                .id(matchId)
                .shareCode("CSGO-QUEUED-PENDING")
                .status(MatchStatus.QUEUED)
                .shareSlug("QUEUED01")
                .players(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.findByShareSlug("QUEUED01")).thenReturn(Optional.of(queued));
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> i.getArgument(0));

        AwardBadgeDto fallbackBadge = new AwardBadgeDto(
                "SOLID_SOLDIER", "Solid Soldier", BadgeCategory.FLEX, "🎖️", "Solid play"
        );
        when(roastBadgeEngine.evaluatePlayer(any(), anyInt())).thenReturn(
                new PlayerEvaluationResult("Solid Soldier", "Clocked in, clocked out.", List.of(fallbackBadge))
        );

        MatchAnalysisDetailDto result = service.getMatchByIdOrSlug("QUEUED01");

        assertThat(result.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(result.teamAPlayers()).hasSize(5);
        assertThat(result.teamBPlayers()).hasSize(5);
        assertThat(result.totalRounds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("deterministic simulation should yield identical match results for the same share code")
    void shouldProduceDeterministicOutputForSameShareCode() {
        String testCode = "CSGO-TEST-DETERMINISTIC-12345";

        MatchAnalysisEntity match1 = MatchAnalysisEntity.builder()
                .id(UUID.randomUUID())
                .shareCode(testCode)
                .status(MatchStatus.QUEUED)
                .shareSlug("DET11111")
                .players(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        MatchAnalysisEntity match2 = MatchAnalysisEntity.builder()
                .id(UUID.randomUUID())
                .shareCode(testCode)
                .status(MatchStatus.QUEUED)
                .shareSlug("DET22222")
                .players(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(roastBadgeEngine.evaluatePlayer(any(), anyInt())).thenReturn(
                new PlayerEvaluationResult("Solid Soldier", "Clocked in, clocked out.", List.of())
        );

        MatchAnalysisDetailDto r1 = service.populateDeterministicAnalysis(match1, testCode, Optional.empty());
        MatchAnalysisDetailDto r2 = service.populateDeterministicAnalysis(match2, testCode, Optional.empty());

        assertThat(r1.mapName()).isEqualTo(r2.mapName());
        assertThat(r1.teamAScore()).isEqualTo(r2.teamAScore());
        assertThat(r1.teamBScore()).isEqualTo(r2.teamBScore());
        assertThat(r1.totalRounds()).isEqualTo(r2.totalRounds());
        assertThat(r1.durationSeconds()).isEqualTo(r2.durationSeconds());
    }

    @Test
    @DisplayName("claimNextQueuedJob should transition oldest QUEUED match to PARSING")
    void shouldClaimNextQueuedJob() {
        MatchAnalysisEntity queued = MatchAnalysisEntity.builder()
                .id(UUID.randomUUID())
                .shareCode("CSGO-QUEUED")
                .status(MatchStatus.QUEUED)
                .shareSlug("QUEUE123")
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.findFirstByStatusOrderByCreatedAtAsc(MatchStatus.QUEUED))
                .thenReturn(Optional.of(queued));
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> i.getArgument(0));

        Optional<ClaimMatchJobResponseDto> claimed = service.claimNextQueuedJob();

        assertThat(claimed).isPresent();
        assertThat(claimed.get().shareSlug()).isEqualTo("QUEUE123");
        assertThat(queued.getStatus()).isEqualTo(MatchStatus.PARSING);
    }

    @Test
    @DisplayName("completeMatchAnalysis should persist players, awards, and mark match COMPLETED")
    void shouldCompleteMatchAnalysis() {
        UUID matchId = UUID.randomUUID();
        MatchAnalysisEntity match = MatchAnalysisEntity.builder()
                .id(matchId)
                .shareCode("CSGO-PARSE-ME")
                .status(MatchStatus.PARSING)
                .shareSlug("PARSE123")
                .players(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerStatsDto player1 = new MatchPlayerStatsDto(
                "76561198000000001", "s1mple", MatchTeamSide.TEAM_A,
                24, 10, 3, 98.0, 70.0, 80.0, 1.45,
                3, 1, 1, 0, 10, 80, 0, 2, 1, 1, 0
        );

        ParsedMatchPayloadDto payload = new ParsedMatchPayloadDto(
                "de_mirage", 13, 9, 2200, List.of(player1)
        );

        AwardBadgeDto badge = new AwardBadgeDto(
                "HEADSHOT_DEMON", "Headshot Demon", BadgeCategory.FLEX, "💀", "70.0% headshot accuracy"
        );
        when(roastBadgeEngine.evaluatePlayer(any(), anyInt())).thenReturn(
                new PlayerEvaluationResult("Headshot Demon", "70.0% headshot accuracy", List.of(badge))
        );
        when(userLookupService.findBySteamId(any())).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> i.getArgument(0));

        MatchAnalysisDetailDto completed = service.completeMatchAnalysis(matchId, payload);

        assertThat(completed.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(completed.totalRounds()).isEqualTo(22);
        assertThat(completed.teamAPlayers()).hasSize(1);
        assertThat(completed.teamAPlayers().get(0).headlineTitle()).isEqualTo("Headshot Demon");
        assertThat(completed.teamAPlayers().get(0).awards()).hasSize(1);
    }

    @Test
    @DisplayName("failMatchAnalysis should set status to FAILED with reason")
    void shouldFailMatchAnalysis() {
        UUID matchId = UUID.randomUUID();
        MatchAnalysisEntity match = MatchAnalysisEntity.builder()
                .id(matchId)
                .shareCode("CSGO-FAIL")
                .status(MatchStatus.PARSING)
                .shareSlug("FAIL1234")
                .createdAt(Instant.now())
                .build();

        when(matchAnalysisRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> i.getArgument(0));

        MatchAnalysisDetailDto failed = service.failMatchAnalysis(
                matchId, new FailMatchJobRequestDto("Demo packet corrupted at tick 4500")
        );

        assertThat(failed.status()).isEqualTo(MatchStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("Demo packet corrupted at tick 4500");
    }

    @Test
    @DisplayName("simulateMatchAnalysis should generate realistic 10-player match")
    void shouldSimulateMatchAnalysis() {
        when(matchAnalysisRepository.findByShareSlug(any())).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any(MatchAnalysisEntity.class))).thenAnswer(i -> {
            MatchAnalysisEntity e = i.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        when(roastBadgeEngine.evaluatePlayer(any(), anyInt())).thenReturn(
                new PlayerEvaluationResult("Solid Soldier", "Clocked in, clocked out.", List.of())
        );

        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), "76561198000000001", "s1mple", AuthenticatedUser.ROLE_USER
        );

        MatchAnalysisDetailDto simulated = service.simulateMatchAnalysis(Optional.of(user), "de_dust2");

        assertThat(simulated).isNotNull();
        assertThat(simulated.teamAPlayers()).hasSize(5);
        assertThat(simulated.teamBPlayers()).hasSize(5);
        assertThat(simulated.totalRounds()).isEqualTo(24);
        assertThat(simulated.mapName()).isEqualTo("de_dust2");
    }

    @Test
    @DisplayName("getPlayerRoastCard should format card payload for player")
    void shouldGetPlayerRoastCard() {
        UUID matchId = UUID.randomUUID();
        MatchAnalysisEntity match = MatchAnalysisEntity.builder()
                .id(matchId)
                .shareCode("CSGO-CARD")
                .status(MatchStatus.COMPLETED)
                .shareSlug("CARD1234")
                .mapName("de_mirage")
                .teamAScore(13)
                .teamBScore(11)
                .totalRounds(24)
                .createdAt(Instant.now())
                .build();

        MatchPlayerEntity player = MatchPlayerEntity.builder()
                .id(UUID.randomUUID())
                .match(match)
                .steamId("76561198000000001")
                .playerName("s1mple")
                .teamSide(MatchTeamSide.TEAM_A)
                .kills(26)
                .deaths(12)
                .assists(4)
                .adr(102.5)
                .headshotPct(73.1)
                .impactRating(1.48)
                .headlineTitle("Headshot Demon")
                .headlineRoast("73.1% headshot accuracy")
                .awards(new ArrayList<>())
                .build();

        match.setPlayers(List.of(player));

        when(matchAnalysisRepository.findByShareSlug("CARD1234")).thenReturn(Optional.of(match));

        PlayerRoastCardDto card = service.getPlayerRoastCard("CARD1234", "76561198000000001");

        assertThat(card).isNotNull();
        assertThat(card.playerName()).isEqualTo("s1mple");
        assertThat(card.scoreLine()).isEqualTo("13 - 11");
        assertThat(card.won()).isTrue();
        assertThat(card.headlineTitle()).isEqualTo("Headshot Demon");
        assertThat(card.shareUrl()).contains("CARD1234");
    }
}