package com.cs2hub.minigames.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.DailyChallengeClipEntity;
import com.cs2hub.minigames.domain.DailyChallengeEntity;
import com.cs2hub.minigames.domain.UserGuessEntity;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.CompletedGuessDto;
import com.cs2hub.minigames.dto.DailyChallengeResponseDto;
import com.cs2hub.minigames.dto.GuessResultResponseDto;
import com.cs2hub.minigames.dto.LeaderboardEntryDto;
import com.cs2hub.minigames.dto.LeaderboardResponseDto;
import com.cs2hub.minigames.dto.RoundClipDto;
import com.cs2hub.minigames.dto.SubmitGuessRequestDto;
import com.cs2hub.minigames.repository.ClipRepository;
import com.cs2hub.minigames.repository.DailyChallengeClipRepository;
import com.cs2hub.minigames.repository.DailyChallengeRepository;
import com.cs2hub.minigames.repository.LeaderboardRowProjection;
import com.cs2hub.minigames.repository.UserGuessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyChallengeService {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final DailyChallengeClipRepository dailyChallengeClipRepository;
    private final ClipRepository clipRepository;
    private final UserGuessRepository userGuessRepository;
    private final UserLookupService userLookupService;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void generateTodayChallengeScheduled() {
        log.info("Running daily challenge generator cron job for UTC midnight");
        generateTodayChallengeIfAbsent();
    }

    @Transactional
    public DailyChallengeEntity generateTodayChallengeIfAbsent() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Optional<DailyChallengeEntity> existing = dailyChallengeRepository.findByChallengeDate(today);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<ClipEntity> availableClips = clipRepository.findTop3ByStatusOrderByCreatedAtAsc(ClipStatus.APPROVED);
        if (availableClips.size() < 3) {
            log.warn("Cannot generate daily challenge for {}: only {} APPROVED clips available (minimum 3 required)",
                    today, availableClips.size());
            return null;
        }

        DailyChallengeEntity challenge = DailyChallengeEntity.builder()
                .id(UUID.randomUUID())
                .challengeDate(today)
                .build();
        challenge = dailyChallengeRepository.save(challenge);

        List<DailyChallengeClipEntity> challengeClips = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ClipEntity clip = availableClips.get(i);
            clip.setStatus(ClipStatus.USED);
            clipRepository.save(clip);

            DailyChallengeClipEntity roundClip = DailyChallengeClipEntity.builder()
                    .id(UUID.randomUUID())
                    .challenge(challenge)
                    .clip(clip)
                    .roundOrder(i + 1)
                    .build();
            challengeClips.add(roundClip);
        }

        dailyChallengeClipRepository.saveAll(challengeClips);
        challenge.setChallengeClips(challengeClips);

        log.info("Successfully generated daily challenge {} for date {}", challenge.getId(), today);
        return challenge;
    }

    @Transactional
    public DailyChallengeEntity manualGenerateTodayChallenge(boolean seedSampleClipsIfEmpty) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Optional<DailyChallengeEntity> existing = dailyChallengeRepository.findByChallengeDate(today);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (seedSampleClipsIfEmpty && clipRepository.countByStatus(ClipStatus.APPROVED) < 3) {
            seedSampleApprovedClips();
        }

        DailyChallengeEntity created = generateTodayChallengeIfAbsent();
        if (created == null) {
            throw new BadRequestException("Not enough APPROVED clips in the pool to generate daily challenge (minimum 3 required)");
        }
        return created;
    }

    @Transactional(readOnly = true)
    public DailyChallengeResponseDto getTodayChallenge(Optional<AuthenticatedUser> currentUser, String guestSessionId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        DailyChallengeEntity challenge = dailyChallengeRepository.findByChallengeDate(today)
                .orElseGet(this::generateTodayChallengeIfAbsent);

        if (challenge == null) {
            throw new ResourceNotFoundException("No active daily challenge available for today");
        }

        List<DailyChallengeClipEntity> roundClips = dailyChallengeClipRepository
                .findAllByChallengeIdOrderByRoundOrderAsc(challenge.getId());

        List<UserGuessEntity> userGuesses = getUserGuesses(challenge.getId(), currentUser, guestSessionId);
        Map<UUID, UserGuessEntity> guessMap = userGuesses.stream()
                .collect(Collectors.toMap(g -> g.getClip().getId(), g -> g, (a, b) -> a));

        List<RoundClipDto> rounds = roundClips.stream()
                .map(rc -> {
                    ClipEntity clip = rc.getClip();
                    UserGuessEntity guess = guessMap.get(clip.getId());

                    // ANTI-CHEAT RULE: Do not expose actual ratings until player submits a guess for this clip
                    Integer actualRating = guess != null ? clip.getActualPremierRating() : null;
                    Integer actualFaceit = guess != null ? clip.getActualFaceitLevel() : null;

                    CompletedGuessDto completedGuess = null;
                    if (guess != null) {
                        int diff = Math.abs(clip.getActualPremierRating() - guess.getGuessedPremierRating());
                        String tierEmoji = EloScoringCalculator.determineTier(clip.getActualPremierRating(), guess.getGuessedPremierRating()).getEmoji();
                        completedGuess = new CompletedGuessDto(
                                guess.getGuessedPremierRating(),
                                guess.getPointsAwarded(),
                                tierEmoji,
                                diff
                        );
                    }

                    return new RoundClipDto(
                            rc.getRoundOrder(),
                            clip.getId(),
                            clip.getTitle(),
                            clip.getVideoUrl(),
                            clip.getVideoProvider(),
                            clip.getMapName(),
                            actualRating,
                            actualFaceit,
                            completedGuess
                    );
                })
                .toList();

        int totalScore = userGuesses.stream().mapToInt(UserGuessEntity::getPointsAwarded).sum();
        boolean completed = !rounds.isEmpty() && rounds.stream().allMatch(r -> r.userGuess() != null);

        // Sort guesses by round order for share summary
        List<UserGuessEntity> orderedGuesses = roundClips.stream()
                .map(rc -> guessMap.get(rc.getClip().getId()))
                .filter(Objects::nonNull)
                .toList();

        String shareText = completed ? EloScoringCalculator.buildShareSummary(challenge.getChallengeDate(), orderedGuesses) : null;

        return new DailyChallengeResponseDto(
                challenge.getId(),
                challenge.getChallengeDate(),
                rounds,
                totalScore,
                completed,
                shareText
        );
    }

    @Transactional
    public GuessResultResponseDto submitGuess(
            Optional<AuthenticatedUser> currentUser,
            String guestSessionId,
            SubmitGuessRequestDto request
    ) {
        if (currentUser.isEmpty() && !StringUtils.hasText(guestSessionId)) {
            throw new BadRequestException("Authentication or guest session header (X-Guest-Session-Id) is required to submit a guess");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        DailyChallengeEntity challenge = dailyChallengeRepository.findByChallengeDate(today)
                .orElseThrow(() -> new ResourceNotFoundException("No active daily challenge for today"));

        List<DailyChallengeClipEntity> roundClips = dailyChallengeClipRepository
                .findAllByChallengeIdOrderByRoundOrderAsc(challenge.getId());

        DailyChallengeClipEntity targetRoundClip = roundClips.stream()
                .filter(rc -> rc.getClip().getId().equals(request.clipId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Specified clip does not belong to today's active challenge"));

        ClipEntity clip = targetRoundClip.getClip();

        // Check if user/guest has already guessed this clip
        if (currentUser.isPresent()) {
            UUID userId = currentUser.get().userId();
            if (userGuessRepository.findByChallengeIdAndClipIdAndUserId(challenge.getId(), clip.getId(), userId).isPresent()) {
                throw new BadRequestException("A guess has already been submitted for this clip in today's challenge");
            }
        } else {
            if (userGuessRepository.findByChallengeIdAndClipIdAndGuestSessionId(challenge.getId(), clip.getId(), guestSessionId).isPresent()) {
                throw new BadRequestException("A guess has already been submitted for this clip in today's challenge");
            }
        }

        EloScoringCalculator.ScoreResult scoreResult = EloScoringCalculator.calculateScore(
                clip.getActualPremierRating(),
                request.guessedPremierRating()
        );

        UserGuessEntity guess = UserGuessEntity.builder()
                .id(UUID.randomUUID())
                .challenge(challenge)
                .clip(clip)
                .userId(currentUser.map(AuthenticatedUser::userId).orElse(null))
                .guestSessionId(currentUser.isEmpty() ? guestSessionId : null)
                .guessedPremierRating(request.guessedPremierRating())
                .pointsAwarded(scoreResult.points())
                .build();

        userGuessRepository.save(guess);

        List<UserGuessEntity> allTodayGuesses = getUserGuesses(challenge.getId(), currentUser, guestSessionId);
        int totalScore = allTodayGuesses.stream().mapToInt(UserGuessEntity::getPointsAwarded).sum();
        boolean completed = allTodayGuesses.size() >= roundClips.size() && roundClips.size() >= 3;

        Map<UUID, UserGuessEntity> guessMap = allTodayGuesses.stream()
                .collect(Collectors.toMap(g -> g.getClip().getId(), g -> g, (a, b) -> a));

        List<UserGuessEntity> orderedGuesses = roundClips.stream()
                .map(rc -> guessMap.get(rc.getClip().getId()))
                .filter(Objects::nonNull)
                .toList();

        String shareText = completed ? EloScoringCalculator.buildShareSummary(challenge.getChallengeDate(), orderedGuesses) : null;

        return new GuessResultResponseDto(
                clip.getId(),
                targetRoundClip.getRoundOrder(),
                clip.getActualPremierRating(),
                clip.getActualFaceitLevel(),
                request.guessedPremierRating(),
                scoreResult.difference(),
                scoreResult.points(),
                scoreResult.tier().getEmoji(),
                totalScore,
                completed,
                shareText
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardResponseDto getLeaderboard(String period, LocalDate date, int limit) {
        String periodUpper = StringUtils.hasText(period) ? period.toUpperCase() : "DAILY";
        int safeLimit = Math.max(1, Math.min(limit > 0 ? limit : 20, 100));

        List<LeaderboardRowProjection> rows;
        LocalDate queryDate = null;

        if ("ALL_TIME".equalsIgnoreCase(periodUpper)) {
            rows = userGuessRepository.findAllTimeLeaderboard(PageRequest.of(0, safeLimit));
        } else {
            queryDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
            Optional<DailyChallengeEntity> challenge = dailyChallengeRepository.findByChallengeDate(queryDate);
            if (challenge.isEmpty()) {
                return new LeaderboardResponseDto("DAILY", queryDate, Collections.emptyList());
            }
            rows = userGuessRepository.findDailyLeaderboard(challenge.get().getId(), PageRequest.of(0, safeLimit));
        }

        if (rows.isEmpty()) {
            return new LeaderboardResponseDto(periodUpper, queryDate, Collections.emptyList());
        }

        Set<UUID> userIds = rows.stream()
                .map(LeaderboardRowProjection::getUserId)
                .collect(Collectors.toSet());

        Map<UUID, UserSummaryDto> userMap = userLookupService.findByIds(userIds);

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int rank = 1;
        for (LeaderboardRowProjection row : rows) {
            UserSummaryDto userSummary = userMap.get(row.getUserId());
            String username = userSummary != null ? userSummary.username() : "Player_" + row.getUserId().toString().substring(0, 6);
            String avatarUrl = userSummary != null ? userSummary.avatarUrl() : null;

            entries.add(new LeaderboardEntryDto(
                    rank++,
                    row.getUserId(),
                    username,
                    avatarUrl,
                    row.getTotalScore() != null ? row.getTotalScore() : 0L,
                    row.getRoundsCount() != null ? row.getRoundsCount() : 0L
            ));
        }

        return new LeaderboardResponseDto(periodUpper, queryDate, entries);
    }

    private List<UserGuessEntity> getUserGuesses(UUID challengeId, Optional<AuthenticatedUser> currentUser, String guestSessionId) {
        if (currentUser.isPresent()) {
            return userGuessRepository.findAllByChallengeIdAndUserId(challengeId, currentUser.get().userId());
        } else if (StringUtils.hasText(guestSessionId)) {
            return userGuessRepository.findAllByChallengeIdAndGuestSessionId(challengeId, guestSessionId);
        }
        return Collections.emptyList();
    }

    private void seedSampleApprovedClips() {
        UUID systemUser = UUID.fromString("00000000-0000-0000-0000-000000000001");

        List<ClipEntity> sampleClips = List.of(
                ClipEntity.builder()
                        .id(UUID.randomUUID())
                        .submittedByUserId(systemUser)
                        .title("Mirage A Site 1v3 Clutch")
                        .videoUrl("https://www.youtube.com/watch?v=sample1")
                        .videoProvider(VideoProvider.YOUTUBE)
                        .mapName("de_mirage")
                        .actualPremierRating(16450)
                        .actualFaceitLevel(7)
                        .status(ClipStatus.APPROVED)
                        .build(),
                ClipEntity.builder()
                        .id(UUID.randomUUID())
                        .submittedByUserId(systemUser)
                        .title("Inferno Banana Anti-Eco Ace")
                        .videoUrl("https://www.youtube.com/watch?v=sample2")
                        .videoProvider(VideoProvider.YOUTUBE)
                        .mapName("de_inferno")
                        .actualPremierRating(19800)
                        .actualFaceitLevel(9)
                        .status(ClipStatus.APPROVED)
                        .build(),
                ClipEntity.builder()
                        .id(UUID.randomUUID())
                        .submittedByUserId(systemUser)
                        .title("Dust2 Mid To B Spray Transfer")
                        .videoUrl("https://www.youtube.com/watch?v=sample3")
                        .videoProvider(VideoProvider.YOUTUBE)
                        .mapName("de_dust2")
                        .actualPremierRating(13200)
                        .actualFaceitLevel(5)
                        .status(ClipStatus.APPROVED)
                        .build()
        );

        clipRepository.saveAll(sampleClips);
        log.info("Seeded 3 sample APPROVED clips for minigames testing");
    }
}
