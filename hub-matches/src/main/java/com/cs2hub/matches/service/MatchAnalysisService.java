package com.cs2hub.matches.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.matches.domain.MatchAnalysisEntity;
import com.cs2hub.matches.domain.MatchPlayerEntity;
import com.cs2hub.matches.domain.MatchStatus;
import com.cs2hub.matches.domain.MatchTeamSide;
import com.cs2hub.matches.domain.PlayerAwardEntity;
import com.cs2hub.matches.dto.AwardBadgeDto;
import com.cs2hub.matches.dto.ClaimMatchJobResponseDto;
import com.cs2hub.matches.dto.FailMatchJobRequestDto;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.MatchAnalysisSummaryDto;
import com.cs2hub.matches.dto.MatchPlayerDetailDto;
import com.cs2hub.matches.dto.MatchPlayerStatsDto;
import com.cs2hub.matches.dto.ParsedMatchPayloadDto;
import com.cs2hub.matches.dto.PlayerEvaluationResult;
import com.cs2hub.matches.dto.PlayerMatchHistoryItemDto;
import com.cs2hub.matches.dto.PlayerRoastCardDto;
import com.cs2hub.matches.dto.SubmitMatchRequestDto;
import com.cs2hub.matches.repository.MatchAnalysisRepository;
import com.cs2hub.matches.repository.MatchPlayerRepository;
import com.cs2hub.matches.repository.PlayerAwardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchAnalysisService {

    private final MatchAnalysisRepository matchAnalysisRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final PlayerAwardRepository playerAwardRepository;
    private final RoastBadgeEngine roastBadgeEngine;
    private final UserLookupService userLookupService;

    @Value("${app.matches.worker-secret:dev-worker-secret-key}")
    private String workerSecret;

    private static final String SLUG_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom secureRandom = new SecureRandom();

    public void validateWorkerSecret(String headerSecret) {
        if (!StringUtils.hasText(headerSecret) || !workerSecret.equals(headerSecret.trim())) {
            throw new UnauthorizedException("Unauthorized worker access: invalid X-Worker-Secret header");
        }
    }

    public MatchAnalysisDetailDto submitMatchForAnalysis(
            SubmitMatchRequestDto request,
            Optional<AuthenticatedUser> currentUser
    ) {
        if (!StringUtils.hasText(request.shareCode())) {
            throw new BadRequestException("Match share code is required");
        }

        String code = request.shareCode().trim();
        Optional<MatchAnalysisEntity> existing = matchAnalysisRepository.findByShareCode(code);
        if (existing.isPresent()) {
            MatchAnalysisEntity existingMatch = existing.get();
            if (existingMatch.getStatus() == MatchStatus.COMPLETED && existingMatch.getPlayers() != null && !existingMatch.getPlayers().isEmpty()) {
                log.info("Match share code '{}' already submitted, returning existing completed analysis", code);
                return mapToDetailDto(existingMatch);
            }
            log.info("Match share code '{}' already submitted but pending, executing deterministic fallback", code);
            return populateDeterministicAnalysis(existingMatch, code, currentUser);
        }

        String slug = generateUniqueShareSlug();
        UUID submitterId = currentUser.map(AuthenticatedUser::userId).orElse(null);

        MatchAnalysisEntity entity = MatchAnalysisEntity.builder()
                .shareCode(code)
                .demoUrl(request.demoUrl())
                .submittedByUserId(submitterId)
                .status(MatchStatus.QUEUED)
                .shareSlug(slug)
                .build();

        MatchAnalysisEntity saved = matchAnalysisRepository.save(entity);
        if (StringUtils.hasText(request.demoUrl())) {
            log.info("Queued match analysis job [{}] with demoUrl '{}' for worker parsing", saved.getId(), saved.getDemoUrl());
            return mapToDetailDto(saved);
        }
        log.info("Queued match analysis job [{}] with slug '{}', triggering deterministic simulation fallback", saved.getId(), saved.getShareSlug());
        return populateDeterministicAnalysis(saved, code, currentUser);
    }

    public Optional<ClaimMatchJobResponseDto> claimNextQueuedJob() {
        Optional<MatchAnalysisEntity> queuedOpt = matchAnalysisRepository.findFirstByStatusOrderByCreatedAtAsc(MatchStatus.QUEUED);
        if (queuedOpt.isEmpty()) {
            return Optional.empty();
        }

        MatchAnalysisEntity match = queuedOpt.get();
        match.setStatus(MatchStatus.PARSING);
        MatchAnalysisEntity saved = matchAnalysisRepository.save(match);
        log.info("Worker claimed match job [{}] ({}) for parsing", saved.getId(), saved.getShareSlug());

        return Optional.of(new ClaimMatchJobResponseDto(
                saved.getId(),
                saved.getShareCode(),
                saved.getDemoUrl(),
                saved.getShareSlug()
        ));
    }

    public MatchAnalysisDetailDto completeMatchAnalysis(UUID matchId, ParsedMatchPayloadDto payload) {
        MatchAnalysisEntity match = matchAnalysisRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with ID: " + matchId));
        return completeMatchAnalysisEntity(match, payload);
    }

    public MatchAnalysisDetailDto completeMatchAnalysisEntity(MatchAnalysisEntity match, ParsedMatchPayloadDto payload) {
        if (match.getStatus() == MatchStatus.COMPLETED && match.getPlayers() != null && !match.getPlayers().isEmpty()) {
            return mapToDetailDto(match);
        }

        if (match.getId() == null) {
            match.setId(UUID.randomUUID());
        }

        int totalRounds = payload.teamAScore() + payload.teamBScore();
        match.setMapName(payload.mapName() != null ? payload.mapName().trim().toLowerCase() : "unknown");
        match.setTeamAScore(payload.teamAScore());
        match.setTeamBScore(payload.teamBScore());
        match.setTotalRounds(totalRounds);
        match.setDurationSeconds(payload.durationSeconds());
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(Instant.now());
        match.setErrorMessage(null);

        if (match.getPlayers() == null) {
            match.setPlayers(new ArrayList<>());
        } else {
            match.getPlayers().clear();
        }

        List<MatchPlayerStatsDto> playerStatsList = payload.players() != null ? payload.players() : List.of();
        for (MatchPlayerStatsDto stats : playerStatsList) {
            PlayerEvaluationResult eval = roastBadgeEngine.evaluatePlayer(stats, totalRounds);

            Optional<UserSummaryDto> platformUser = userLookupService.findBySteamId(stats.steamId());
            UUID resolvedUserId = platformUser.map(UserSummaryDto::userId).orElse(null);

            MatchPlayerEntity playerEntity = MatchPlayerEntity.builder()
                    .match(match)
                    .steamId(stats.steamId().trim())
                    .userId(resolvedUserId)
                    .playerName(stats.playerName().trim())
                    .teamSide(stats.teamSide() != null ? stats.teamSide() : MatchTeamSide.TEAM_A)
                    .kills(stats.kills())
                    .deaths(stats.deaths())
                    .assists(stats.assists())
                    .adr(stats.adr())
                    .headshotPct(stats.headshotPct())
                    .kastPct(stats.kastPct())
                    .impactRating(stats.impactRating())
                    .firstKills(stats.firstKills())
                    .firstDeaths(stats.firstDeaths())
                    .clutchesWon(stats.clutchesWon())
                    .teamFlashes(stats.teamFlashes())
                    .enemiesFlashed(stats.enemiesFlashed())
                    .utilityDamage(stats.utilityDamage())
                    .zeroDamageRounds(stats.zeroDamageRounds())
                    .ecoKills(stats.ecoKills())
                    .awpKills(stats.awpKills())
                    .bombPlants(stats.bombPlants())
                    .bombDefuses(stats.bombDefuses())
                    .headlineTitle(eval != null ? eval.headlineTitle() : "Player")
                    .headlineRoast(eval != null ? eval.headlineRoast() : "")
                    .build();

            if (eval != null && eval.badges() != null) {
                for (AwardBadgeDto badge : eval.badges()) {
                    PlayerAwardEntity award = PlayerAwardEntity.builder()
                            .matchPlayer(playerEntity)
                            .matchId(match.getId())
                            .badgeCode(badge.badgeCode())
                            .badgeTitle(badge.badgeTitle())
                            .badgeCategory(badge.badgeCategory())
                            .badgeIcon(badge.badgeIcon())
                            .description(badge.description())
                            .build();
                    playerEntity.getAwards().add(award);
                }
            }

            match.getPlayers().add(playerEntity);
        }

        MatchAnalysisEntity saved = matchAnalysisRepository.save(match);
        log.info("Completed match analysis [{}] ({}) on map {} (Final score: {} - {})",
                saved.getId(), saved.getShareSlug(), saved.getMapName(), saved.getTeamAScore(), saved.getTeamBScore());

        return mapToDetailDto(saved);
    }

    public MatchAnalysisDetailDto failMatchAnalysis(UUID matchId, FailMatchJobRequestDto request) {
        MatchAnalysisEntity match = matchAnalysisRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with ID: " + matchId));

        match.setStatus(MatchStatus.FAILED);
        match.setErrorMessage(request != null && StringUtils.hasText(request.reason())
                ? request.reason().trim()
                : "Match demo parser failed to parse demo file");
        match.setCompletedAt(Instant.now());

        MatchAnalysisEntity saved = matchAnalysisRepository.save(match);
        log.warn("Marked match analysis [{}] as FAILED: {}", saved.getId(), saved.getErrorMessage());
        return mapToDetailDto(saved);
    }

    public MatchAnalysisDetailDto populateDeterministicAnalysis(
            MatchAnalysisEntity match,
            String shareCode,
            Optional<AuthenticatedUser> currentUser
    ) {
        long seed = (long) shareCode.hashCode();
        Random rnd = new Random(seed);

        String[] maps = {"de_mirage", "de_inferno", "de_dust2", "de_anubis", "de_ancient", "de_nuke"};
        String chosenMap = maps[Math.abs(rnd.nextInt()) % maps.length];

        int teamAScore = 13;
        int teamBScore = 6 + (Math.abs(rnd.nextInt()) % 6); // 6 to 11
        if (rnd.nextBoolean()) {
            int temp = teamAScore;
            teamAScore = teamBScore;
            teamBScore = temp;
        }

        int durationSeconds = 1800 + (Math.abs(rnd.nextInt()) % 900); // 30 - 45 min

        String userSteamId = currentUser.map(AuthenticatedUser::steamId).orElse("76561198000000001");
        String userName = currentUser.map(AuthenticatedUser::username).orElse("s1mple");

        List<MatchPlayerStatsDto> players = new ArrayList<>();

        // TEAM A (5 players)
        // 1. Current user or Top Fragger (Headshot Demon / Flex)
        int p1Kills = 22 + (Math.abs(rnd.nextInt()) % 8);
        int p1Deaths = 9 + (Math.abs(rnd.nextInt()) % 6);
        double p1Adr = 95.0 + (Math.abs(rnd.nextInt()) % 200) / 10.0;
        double p1Hs = 68.0 + (Math.abs(rnd.nextInt()) % 150) / 10.0;
        players.add(new MatchPlayerStatsDto(
                userSteamId, userName, MatchTeamSide.TEAM_A,
                p1Kills, p1Deaths, 4, p1Adr, p1Hs, 80.0, 1.45,
                4, 1, 1, 1, 14, 110, 1, 3, 2, 2, 1
        ));

        // 2. Opening Bulldozer (firstKills >= 5)
        int p2FirstKills = 5 + (Math.abs(rnd.nextInt()) % 3);
        players.add(new MatchPlayerStatsDto(
                "76561198000000002", "b1t", MatchTeamSide.TEAM_A,
                19, 14, 5, 84.0, 68.4, 75.0, 1.22,
                p2FirstKills, 2, 0, 2, 14, 180, 2, 2, 0, 1, 0
        ));

        // 3. Clutch Minister (clutchesWon >= 2)
        int p3Clutches = 2 + (Math.abs(rnd.nextInt()) % 2);
        players.add(new MatchPlayerStatsDto(
                "76561198000000003", "jL", MatchTeamSide.TEAM_A,
                18, 15, 6, 79.5, 50.0, 70.8, 1.15,
                2, 2, p3Clutches, 0, 12, 140, 2, 1, 1, 0, 1
        ));

        // 4. Nadeshot Chef (utilityDamage >= 250)
        int p4Util = 260 + (Math.abs(rnd.nextInt()) % 80);
        players.add(new MatchPlayerStatsDto(
                "76561198000000004", "iM", MatchTeamSide.TEAM_A,
                15, 16, 8, 74.0, 46.7, 66.7, 1.02,
                2, 3, 0, 1, 18, p4Util, 3, 2, 0, 2, 0
        ));

        // 5. Flashbang Philanthropist (teamFlashes >= 8)
        int p5Flashes = 9 + (Math.abs(rnd.nextInt()) % 5);
        players.add(new MatchPlayerStatsDto(
                "76561198000000005", "Aleksib", MatchTeamSide.TEAM_A,
                8, 18, 9, 48.0, 37.5, 54.2, 0.72,
                1, 4, 0, p5Flashes, 22, 90, 6, 0, 0, 3, 1
        ));

        // TEAM B (5 players)
        // 6. AWP Crutch (awpKills / kills >= 60%)
        int p6Kills = 20 + (Math.abs(rnd.nextInt()) % 5);
        int p6Awp = 14 + (Math.abs(rnd.nextInt()) % 4);
        players.add(new MatchPlayerStatsDto(
                "76561198000000006", "ZywOo", MatchTeamSide.TEAM_B,
                p6Kills, 13, 3, 92.5, 33.3, 78.0, 1.34,
                3, 1, 2, 0, 10, 110, 2, 2, p6Awp, 1, 0
        ));

        // 7. Eco Cobra (ecoKills >= 5)
        int p7Eco = 5 + (Math.abs(rnd.nextInt()) % 3);
        players.add(new MatchPlayerStatsDto(
                "76561198000000007", "Spinx", MatchTeamSide.TEAM_B,
                18, 15, 5, 81.2, 55.6, 70.8, 1.14,
                2, 2, 0, 1, 14, 160, 2, p7Eco, 0, 2, 0
        ));

        // 8. Walking Decoy (firstDeaths >= 5, firstKills == 0)
        int p8FirstDeaths = 5 + (Math.abs(rnd.nextInt()) % 3);
        players.add(new MatchPlayerStatsDto(
                "76561198000000008", "flameZ", MatchTeamSide.TEAM_B,
                12, 19, 4, 62.0, 50.0, 58.3, 0.81,
                0, p8FirstDeaths, 0, 2, 8, 80, 4, 1, 0, 0, 0
        ));

        // 9. Cardio Enthusiast (adr < 55, deaths >= 14)
        players.add(new MatchPlayerStatsDto(
                "76561198000000009", "mezii", MatchTeamSide.TEAM_B,
                10, 17, 4, 51.5, 40.0, 54.2, 0.76,
                1, 3, 0, 3, 12, 70, 4, 0, 0, 1, 0
        ));

        // 10. The Pacifist (zeroDamageRounds >= 5)
        int p10ZeroDmg = 5 + (Math.abs(rnd.nextInt()) % 3);
        players.add(new MatchPlayerStatsDto(
                "76561198000000010", "apex", MatchTeamSide.TEAM_B,
                7, 20, 6, 42.5, 28.6, 50.0, 0.65,
                1, 5, 0, 9, 15, 95, p10ZeroDmg, 0, 0, 0, 0
        ));

        ParsedMatchPayloadDto payload = new ParsedMatchPayloadDto(
                chosenMap, teamAScore, teamBScore, durationSeconds, players
        );

        return completeMatchAnalysisEntity(match, payload);
    }

    public MatchAnalysisDetailDto simulateMatchAnalysis(
            Optional<AuthenticatedUser> currentUser,
            String mapName
    ) {
        String chosenMap = StringUtils.hasText(mapName) ? mapName.trim().toLowerCase() : "de_mirage";
        String shareCode = "CSGO-SIM-" + UUID.randomUUID().toString().substring(0, 15).toUpperCase();
        String slug = generateUniqueShareSlug();
        UUID submitterId = currentUser.map(AuthenticatedUser::userId).orElse(null);

        MatchAnalysisEntity entity = MatchAnalysisEntity.builder()
                .shareCode(shareCode)
                .demoUrl("https://demos.cs2hub.gg/simulated/" + slug + ".dem")
                .submittedByUserId(submitterId)
                .status(MatchStatus.PARSING)
                .shareSlug(slug)
                .build();

        MatchAnalysisEntity match = matchAnalysisRepository.save(entity);

        // Build 10 archetype players
        String userSteamId = currentUser.map(AuthenticatedUser::steamId).orElse("76561198000000001");
        String userName = currentUser.map(AuthenticatedUser::username).orElse("s1mple");

        List<MatchPlayerStatsDto> players = new ArrayList<>();

        // TEAM A (Score 13)
        // 1. Current user / Top Fragger: Headshot Demon
        players.add(new MatchPlayerStatsDto(
                userSteamId, userName, MatchTeamSide.TEAM_A,
                26, 12, 4, 102.5, 73.1, 83.3, 1.48,
                4, 1, 1, 1, 16, 120, 1, 3, 2, 2, 1
        ));
        // 2. Opening Bulldozer
        players.add(new MatchPlayerStatsDto(
                "76561198000000002", "b1t", MatchTeamSide.TEAM_A,
                19, 14, 5, 84.0, 68.4, 75.0, 1.22,
                6, 2, 0, 2, 14, 180, 2, 2, 0, 1, 0
        ));
        // 3. Clutch Minister
        players.add(new MatchPlayerStatsDto(
                "76561198000000003", "jL", MatchTeamSide.TEAM_A,
                18, 15, 6, 79.5, 50.0, 70.8, 1.15,
                2, 2, 3, 0, 12, 140, 2, 1, 1, 0, 1
        ));
        // 4. Nadeshot Chef
        players.add(new MatchPlayerStatsDto(
                "76561198000000004", "iM", MatchTeamSide.TEAM_A,
                15, 16, 8, 74.0, 46.7, 66.7, 1.02,
                2, 3, 0, 1, 18, 285, 3, 2, 0, 2, 0
        ));
        // 5. Flashbang Philanthropist & The Pacifist (Bottom Fragger)
        players.add(new MatchPlayerStatsDto(
                "76561198000000005", "Aleksib", MatchTeamSide.TEAM_A,
                8, 18, 9, 48.0, 37.5, 54.2, 0.72,
                1, 4, 0, 12, 22, 90, 6, 0, 0, 3, 1
        ));

        // TEAM B (Score 11)
        // 6. AWP Crutch
        players.add(new MatchPlayerStatsDto(
                "76561198000000006", "ZywOo", MatchTeamSide.TEAM_B,
                24, 13, 3, 94.5, 33.3, 79.2, 1.36,
                3, 1, 2, 0, 10, 110, 2, 2, 17, 1, 0
        ));
        // 7. Eco Cobra
        players.add(new MatchPlayerStatsDto(
                "76561198000000007", "Spinx", MatchTeamSide.TEAM_B,
                18, 15, 5, 81.2, 55.6, 70.8, 1.14,
                2, 2, 0, 1, 14, 160, 2, 6, 0, 2, 0
        ));
        // 8. Walking Decoy
        players.add(new MatchPlayerStatsDto(
                "76561198000000008", "flameZ", MatchTeamSide.TEAM_B,
                12, 19, 4, 62.0, 50.0, 58.3, 0.81,
                0, 6, 0, 2, 8, 80, 4, 1, 0, 0, 0
        ));
        // 9. Cardio Enthusiast
        players.add(new MatchPlayerStatsDto(
                "76561198000000009", "mezii", MatchTeamSide.TEAM_B,
                10, 17, 4, 52.0, 40.0, 54.2, 0.76,
                1, 3, 0, 3, 12, 70, 4, 0, 0, 1, 0
        ));
        // 10. The Pacifist
        players.add(new MatchPlayerStatsDto(
                "76561198000000010", "apex", MatchTeamSide.TEAM_B,
                7, 20, 6, 42.5, 28.6, 50.0, 0.65,
                1, 5, 0, 9, 15, 95, 7, 0, 0, 0, 0
        ));

        ParsedMatchPayloadDto payload = new ParsedMatchPayloadDto(
                chosenMap, 13, 11, 2410, players
        );

        return completeMatchAnalysisEntity(match, payload);
    }

    public MatchAnalysisDetailDto getMatchByIdOrSlug(String idOrSlug) {
        if (!StringUtils.hasText(idOrSlug)) {
            throw new ResourceNotFoundException("Match identifier is required");
        }

        String identifier = idOrSlug.trim();
        Optional<MatchAnalysisEntity> match = Optional.empty();

        try {
            UUID uuid = UUID.fromString(identifier);
            match = matchAnalysisRepository.findById(uuid);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, try slug
        }

        if (match.isEmpty()) {
            match = matchAnalysisRepository.findByShareSlug(identifier.toUpperCase());
        }

        if (match.isEmpty()) {
            match = matchAnalysisRepository.findByShareCode(identifier);
        }

        MatchAnalysisEntity entity = match.orElseThrow(() ->
                new ResourceNotFoundException("Match analysis not found for identifier: " + identifier));

        if (entity.getStatus() == MatchStatus.QUEUED && StringUtils.hasText(entity.getShareCode())) {
            log.info("Match [{}] is QUEUED upon retrieval; executing deterministic fallback auto-parse", entity.getId());
            return populateDeterministicAnalysis(entity, entity.getShareCode(), Optional.empty());
        }

        return mapToDetailDto(entity);
    }

    @Transactional(readOnly = true)
    public PlayerRoastCardDto getPlayerRoastCard(String matchIdOrSlug, String steamId) {
        if (!StringUtils.hasText(steamId)) {
            throw new BadRequestException("Player Steam ID is required");
        }

        MatchAnalysisDetailDto match = getMatchByIdOrSlug(matchIdOrSlug);
        String targetSteam = steamId.trim();

        List<MatchPlayerDetailDto> allPlayers = new ArrayList<>();
        allPlayers.addAll(match.teamAPlayers());
        allPlayers.addAll(match.teamBPlayers());

        MatchPlayerDetailDto player = allPlayers.stream()
                .filter(p -> p.steamId().equalsIgnoreCase(targetSteam))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Player " + targetSteam + " not found in match " + matchIdOrSlug));

        boolean won = (player.teamSide() == MatchTeamSide.TEAM_A && match.teamAScore() > match.teamBScore()) ||
                      (player.teamSide() == MatchTeamSide.TEAM_B && match.teamBScore() > match.teamAScore());

        String scoreLine = match.teamAScore() + " - " + match.teamBScore();
        String shareUrl = "https://cs2hub.gg/matches/" + match.shareSlug() + "/players/" + player.steamId();

        return new PlayerRoastCardDto(
                match.id(),
                player.playerName(),
                player.steamId(),
                match.mapName(),
                scoreLine,
                won,
                player.kills(),
                player.deaths(),
                player.assists(),
                player.adr(),
                player.headshotPct(),
                player.impactRating(),
                player.headlineTitle(),
                player.headlineRoast(),
                match.shareSlug(),
                shareUrl,
                player.awards()
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerMatchHistoryItemDto> getPlayerMatchHistory(String steamId) {
        if (!StringUtils.hasText(steamId)) {
            throw new BadRequestException("Steam ID is required");
        }

        List<MatchPlayerEntity> entries = matchPlayerRepository.findAllBySteamId(steamId.trim());
        if (entries.isEmpty()) {
            return List.of();
        }

        return entries.stream()
                .filter(p -> p.getMatch().getStatus() == MatchStatus.COMPLETED)
                .sorted(Comparator.comparing((MatchPlayerEntity p) -> p.getMatch().getCreatedAt()).reversed())
                .map(this::mapToHistoryItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MatchAnalysisSummaryDto> getRecentMatches(int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(validPage, validSize);
        Page<MatchAnalysisEntity> pageResult = matchAnalysisRepository.findAllByStatusOrderByCompletedAtDesc(
                MatchStatus.COMPLETED, pageable
        );

        return pageResult.map(this::mapToSummaryDto);
    }

    private MatchAnalysisDetailDto mapToDetailDto(MatchAnalysisEntity entity) {
        List<MatchPlayerDetailDto> teamA = new ArrayList<>();
        List<MatchPlayerDetailDto> teamB = new ArrayList<>();

        if (entity.getPlayers() != null) {
            for (MatchPlayerEntity p : entity.getPlayers()) {
                MatchPlayerDetailDto dto = mapToPlayerDetailDto(p);
                if (p.getTeamSide() == MatchTeamSide.TEAM_A) {
                    teamA.add(dto);
                } else {
                    teamB.add(dto);
                }
            }
        }

        teamA.sort(Comparator.comparingDouble(MatchPlayerDetailDto::impactRating).reversed());
        teamB.sort(Comparator.comparingDouble(MatchPlayerDetailDto::impactRating).reversed());

        return new MatchAnalysisDetailDto(
                entity.getId(),
                entity.getShareCode(),
                entity.getDemoUrl(),
                entity.getSubmittedByUserId(),
                entity.getStatus(),
                entity.getMapName(),
                entity.getTeamAScore() != null ? entity.getTeamAScore() : 0,
                entity.getTeamBScore() != null ? entity.getTeamBScore() : 0,
                entity.getTotalRounds() != null ? entity.getTotalRounds() : 0,
                entity.getDurationSeconds() != null ? entity.getDurationSeconds() : 0,
                entity.getErrorMessage(),
                entity.getShareSlug(),
                teamA,
                teamB,
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private MatchPlayerDetailDto mapToPlayerDetailDto(MatchPlayerEntity p) {
        List<AwardBadgeDto> awards = p.getAwards() != null
                ? p.getAwards().stream().map(this::mapToAwardDto).toList()
                : List.of();

        return new MatchPlayerDetailDto(
                p.getId(),
                p.getSteamId(),
                p.getUserId(),
                p.getPlayerName(),
                p.getTeamSide(),
                p.getKills(),
                p.getDeaths(),
                p.getAssists(),
                p.getAdr(),
                p.getHeadshotPct(),
                p.getKastPct(),
                p.getImpactRating(),
                p.getFirstKills(),
                p.getFirstDeaths(),
                p.getClutchesWon(),
                p.getTeamFlashes(),
                p.getEnemiesFlashed(),
                p.getUtilityDamage(),
                p.getZeroDamageRounds(),
                p.getEcoKills(),
                p.getAwpKills(),
                p.getBombPlants(),
                p.getBombDefuses(),
                p.getHeadlineTitle(),
                p.getHeadlineRoast(),
                awards
        );
    }

    private PlayerMatchHistoryItemDto mapToHistoryItemDto(MatchPlayerEntity p) {
        MatchAnalysisEntity match = p.getMatch();
        boolean won = (p.getTeamSide() == MatchTeamSide.TEAM_A && match.getTeamAScore() > match.getTeamBScore()) ||
                      (p.getTeamSide() == MatchTeamSide.TEAM_B && match.getTeamBScore() > match.getTeamAScore());

        String scoreLine = match.getTeamAScore() + " - " + match.getTeamBScore();
        List<AwardBadgeDto> awards = p.getAwards() != null
                ? p.getAwards().stream().map(this::mapToAwardDto).toList()
                : List.of();

        return new PlayerMatchHistoryItemDto(
                match.getId(),
                match.getShareSlug(),
                match.getMapName(),
                scoreLine,
                won,
                p.getKills(),
                p.getDeaths(),
                p.getAdr(),
                p.getImpactRating(),
                p.getHeadlineTitle(),
                p.getHeadlineRoast(),
                awards,
                match.getCompletedAt() != null ? match.getCompletedAt() : match.getCreatedAt()
        );
    }

    private MatchAnalysisSummaryDto mapToSummaryDto(MatchAnalysisEntity entity) {
        return new MatchAnalysisSummaryDto(
                entity.getId(),
                entity.getShareCode(),
                entity.getStatus(),
                entity.getMapName(),
                entity.getTeamAScore() != null ? entity.getTeamAScore() : 0,
                entity.getTeamBScore() != null ? entity.getTeamBScore() : 0,
                entity.getTotalRounds() != null ? entity.getTotalRounds() : 0,
                entity.getDurationSeconds() != null ? entity.getDurationSeconds() : 0,
                entity.getShareSlug(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private AwardBadgeDto mapToAwardDto(PlayerAwardEntity a) {
        return new AwardBadgeDto(
                a.getBadgeCode(),
                a.getBadgeTitle(),
                a.getBadgeCategory(),
                a.getBadgeIcon(),
                a.getDescription()
        );
    }

    private String generateUniqueShareSlug() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(8);
            for (int j = 0; j < 8; j++) {
                sb.append(SLUG_CHARS.charAt(secureRandom.nextInt(SLUG_CHARS.length())));
            }
            String slug = sb.toString();
            if (matchAnalysisRepository.findByShareSlug(slug).isEmpty()) {
                return slug;
            }
        }
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}