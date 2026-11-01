package com.cs2hub.minigames.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.DailyChallengeClipEntity;
import com.cs2hub.minigames.domain.DailyChallengeEntity;
import com.cs2hub.minigames.domain.UserGuessEntity;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.DailyChallengeResponseDto;
import com.cs2hub.minigames.dto.GuessResultResponseDto;
import com.cs2hub.minigames.dto.LeaderboardResponseDto;
import com.cs2hub.minigames.dto.SubmitGuessRequestDto;
import com.cs2hub.minigames.repository.ClipRepository;
import com.cs2hub.minigames.repository.DailyChallengeClipRepository;
import com.cs2hub.minigames.repository.DailyChallengeRepository;
import com.cs2hub.minigames.repository.LeaderboardRowProjection;
import com.cs2hub.minigames.repository.UserGuessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyChallengeServiceTest {

    @Mock
    private DailyChallengeRepository dailyChallengeRepository;

    @Mock
    private DailyChallengeClipRepository dailyChallengeClipRepository;

    @Mock
    private ClipRepository clipRepository;

    @Mock
    private UserGuessRepository userGuessRepository;

    @Mock
    private UserLookupService userLookupService;

    private DailyChallengeService dailyChallengeService;

    @BeforeEach
    void setUp() {
        dailyChallengeService = new DailyChallengeService(
                dailyChallengeRepository,
                dailyChallengeClipRepository,
                clipRepository,
                userGuessRepository,
                userLookupService
        );
    }

    @Test
    @DisplayName("Should generate today's challenge from 3 approved community clips and update their status to USED")
    void shouldGenerateChallengeFromApprovedClips() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.empty());

        ClipEntity c1 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(false).build();
        ClipEntity c2 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(false).build();
        ClipEntity c3 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(false).build();

        when(clipRepository.findTop3ByStatusAndIsCuratedFalseOrderByCreatedAtAsc(ClipStatus.APPROVED)).thenReturn(List.of(c1, c2, c3));
        when(dailyChallengeRepository.save(any(DailyChallengeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dailyChallengeClipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyChallengeEntity challenge = dailyChallengeService.generateTodayChallengeIfAbsent();

        assertThat(challenge).isNotNull();
        assertThat(challenge.getChallengeDate()).isEqualTo(today);
        assertThat(c1.getStatus()).isEqualTo(ClipStatus.USED);
        assertThat(c2.getStatus()).isEqualTo(ClipStatus.USED);
        assertThat(c3.getStatus()).isEqualTo(ClipStatus.USED);
        assertThat(challenge.getChallengeClips()).hasSize(3);
        verify(dailyChallengeRepository).save(any(DailyChallengeEntity.class));
        verify(clipRepository, times(3)).save(any(ClipEntity.class));
        verify(dailyChallengeClipRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should fallback to curated clips when community clips are fewer than 3")
    void shouldFallbackToCuratedClipsWhenCommunityClipsInsufficient() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.empty());
        when(dailyChallengeRepository.findByChallengeDate(today.minusDays(1))).thenReturn(Optional.empty());

        // 1 community clip
        ClipEntity comm1 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(false).build();
        when(clipRepository.findTop3ByStatusAndIsCuratedFalseOrderByCreatedAtAsc(ClipStatus.APPROVED)).thenReturn(List.of(comm1));

        // 3 curated clips available
        ClipEntity cur1 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(true).build();
        ClipEntity cur2 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(true).build();
        ClipEntity cur3 = ClipEntity.builder().id(UUID.randomUUID()).status(ClipStatus.APPROVED).isCurated(true).build();
        when(clipRepository.findAllByStatusAndIsCuratedTrue(ClipStatus.APPROVED)).thenReturn(List.of(cur1, cur2, cur3));

        when(dailyChallengeRepository.save(any(DailyChallengeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dailyChallengeClipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyChallengeEntity challenge = dailyChallengeService.generateTodayChallengeIfAbsent();

        assertThat(challenge).isNotNull();
        assertThat(challenge.getChallengeClips()).hasSize(3);

        // Community clip was marked USED
        assertThat(comm1.getStatus()).isEqualTo(ClipStatus.USED);
        verify(clipRepository, times(1)).save(comm1);

        // Curated clips remained APPROVED and were not saved as USED
        assertThat(cur1.getStatus()).isEqualTo(ClipStatus.APPROVED);
        assertThat(cur2.getStatus()).isEqualTo(ClipStatus.APPROVED);
        assertThat(cur3.getStatus()).isEqualTo(ClipStatus.APPROVED);
        verify(clipRepository, never()).save(cur1);
        verify(clipRepository, never()).save(cur2);
        verify(clipRepository, never()).save(cur3);
    }

    @Test
    @DisplayName("Anti-Cheat Rule: actualPremierRating must be masked (null) before guessing and revealed after guessing")
    void shouldEnforceAntiCheatMasking() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID challengeId = UUID.randomUUID();
        DailyChallengeEntity challenge = DailyChallengeEntity.builder().id(challengeId).challengeDate(today).build();

        UUID clip1Id = UUID.randomUUID();
        UUID clip2Id = UUID.randomUUID();

        ClipEntity clip1 = ClipEntity.builder()
                .id(clip1Id)
                .title("Clip 1")
                .videoUrl("https://youtube.com/1")
                .videoProvider(VideoProvider.YOUTUBE)
                .mapName("de_mirage")
                .actualPremierRating(16500)
                .actualFaceitLevel(7)
                .build();

        ClipEntity clip2 = ClipEntity.builder()
                .id(clip2Id)
                .title("Clip 2")
                .videoUrl("https://youtube.com/2")
                .videoProvider(VideoProvider.YOUTUBE)
                .mapName("de_inferno")
                .actualPremierRating(21000)
                .actualFaceitLevel(10)
                .build();

        DailyChallengeClipEntity rc1 = DailyChallengeClipEntity.builder().challenge(challenge).clip(clip1).roundOrder(1).build();
        DailyChallengeClipEntity rc2 = DailyChallengeClipEntity.builder().challenge(challenge).clip(clip2).roundOrder(2).build();

        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(challenge));
        when(dailyChallengeClipRepository.findAllByChallengeIdOrderByRoundOrderAsc(challengeId)).thenReturn(List.of(rc1, rc2));

        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "shox", "USER");

        // Player has guessed round 1, but NOT round 2
        UserGuessEntity guess1 = UserGuessEntity.builder()
                .challenge(challenge)
                .clip(clip1)
                .userId(userId)
                .guessedPremierRating(16000)
                .pointsAwarded(1000)
                .build();

        when(userGuessRepository.findAllByChallengeIdAndUserId(challengeId, userId)).thenReturn(List.of(guess1));

        DailyChallengeResponseDto response = dailyChallengeService.getTodayChallenge(Optional.of(user), null);

        assertThat(response).isNotNull();
        assertThat(response.rounds()).hasSize(2);

        // Round 1 was guessed: ratings REVEALED
        assertThat(response.rounds().get(0).actualPremierRating()).isEqualTo(16500);
        assertThat(response.rounds().get(0).actualFaceitLevel()).isEqualTo(7);
        assertThat(response.rounds().get(0).userGuess()).isNotNull();

        // Round 2 not guessed yet: ratings MASKED (Anti-Cheat!)
        assertThat(response.rounds().get(1).actualPremierRating()).isNull();
        assertThat(response.rounds().get(1).actualFaceitLevel()).isNull();
        assertThat(response.rounds().get(1).userGuess()).isNull();
    }

    @Test
    @DisplayName("Should submit guess, evaluate points and reveal actual ratings")
    void shouldSubmitGuessAndScore() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID challengeId = UUID.randomUUID();
        DailyChallengeEntity challenge = DailyChallengeEntity.builder().id(challengeId).challengeDate(today).build();

        UUID clipId = UUID.randomUUID();
        ClipEntity clip = ClipEntity.builder()
                .id(clipId)
                .actualPremierRating(18000)
                .actualFaceitLevel(8)
                .build();

        DailyChallengeClipEntity rc = DailyChallengeClipEntity.builder().challenge(challenge).clip(clip).roundOrder(1).build();

        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(challenge));
        when(dailyChallengeClipRepository.findAllByChallengeIdOrderByRoundOrderAsc(challengeId)).thenReturn(List.of(rc));

        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "shox", "USER");

        when(userGuessRepository.findByChallengeIdAndClipIdAndUserId(challengeId, clipId, userId)).thenReturn(Optional.empty());

        UserGuessEntity savedGuess = UserGuessEntity.builder()
                .challenge(challenge)
                .clip(clip)
                .userId(userId)
                .guessedPremierRating(18200)
                .pointsAwarded(1000)
                .build();

        when(userGuessRepository.findAllByChallengeIdAndUserId(challengeId, userId)).thenReturn(List.of(savedGuess));

        SubmitGuessRequestDto request = new SubmitGuessRequestDto(clipId, 18200);
        GuessResultResponseDto result = dailyChallengeService.submitGuess(Optional.of(user), null, request);

        assertThat(result.actualPremierRating()).isEqualTo(18000);
        assertThat(result.actualFaceitLevel()).isEqualTo(8);
        assertThat(result.difference()).isEqualTo(200);
        assertThat(result.pointsAwarded()).isEqualTo(1000);
        assertThat(result.tierEmoji()).isEqualTo("🟢");
        verify(userGuessRepository).save(any(UserGuessEntity.class));
    }

    @Test
    @DisplayName("Should support guest session guess via X-Guest-Session-Id header")
    void shouldSupportGuestGuess() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID challengeId = UUID.randomUUID();
        DailyChallengeEntity challenge = DailyChallengeEntity.builder().id(challengeId).challengeDate(today).build();

        UUID clipId = UUID.randomUUID();
        ClipEntity clip = ClipEntity.builder()
                .id(clipId)
                .actualPremierRating(15000)
                .build();

        DailyChallengeClipEntity rc = DailyChallengeClipEntity.builder().challenge(challenge).clip(clip).roundOrder(1).build();

        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(challenge));
        when(dailyChallengeClipRepository.findAllByChallengeIdOrderByRoundOrderAsc(challengeId)).thenReturn(List.of(rc));

        String guestSessionId = "guest_abc_123";
        when(userGuessRepository.findByChallengeIdAndClipIdAndGuestSessionId(challengeId, clipId, guestSessionId))
                .thenReturn(Optional.empty());

        UserGuessEntity guestGuess = UserGuessEntity.builder()
                .challenge(challenge)
                .clip(clip)
                .guestSessionId(guestSessionId)
                .guessedPremierRating(15500)
                .pointsAwarded(1000)
                .build();

        when(userGuessRepository.findAllByChallengeIdAndGuestSessionId(challengeId, guestSessionId))
                .thenReturn(List.of(guestGuess));

        SubmitGuessRequestDto request = new SubmitGuessRequestDto(clipId, 15500);
        GuessResultResponseDto result = dailyChallengeService.submitGuess(Optional.empty(), guestSessionId, request);

        assertThat(result.pointsAwarded()).isEqualTo(1000);
        assertThat(result.difference()).isEqualTo(500);
        verify(userGuessRepository).save(any(UserGuessEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate guess for the same clip in today's challenge")
    void shouldRejectDuplicateGuess() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UUID challengeId = UUID.randomUUID();
        DailyChallengeEntity challenge = DailyChallengeEntity.builder().id(challengeId).challengeDate(today).build();

        UUID clipId = UUID.randomUUID();
        ClipEntity clip = ClipEntity.builder().id(clipId).actualPremierRating(15000).build();
        DailyChallengeClipEntity rc = DailyChallengeClipEntity.builder().challenge(challenge).clip(clip).roundOrder(1).build();

        when(dailyChallengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(challenge));
        when(dailyChallengeClipRepository.findAllByChallengeIdOrderByRoundOrderAsc(challengeId)).thenReturn(List.of(rc));

        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "shox", "USER");

        UserGuessEntity existingGuess = UserGuessEntity.builder().build();
        when(userGuessRepository.findByChallengeIdAndClipIdAndUserId(challengeId, clipId, userId))
                .thenReturn(Optional.of(existingGuess));

        SubmitGuessRequestDto request = new SubmitGuessRequestDto(clipId, 15000);

        assertThatThrownBy(() -> dailyChallengeService.submitGuess(Optional.of(user), null, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been submitted");
    }

    @Test
    @DisplayName("Should enrich leaderboard entries via UserLookupService")
    void shouldEnrichLeaderboardViaSPI() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        LeaderboardRowProjection row1 = new TestLeaderboardRow(user1Id, 2800L, 3L);
        LeaderboardRowProjection row2 = new TestLeaderboardRow(user2Id, 2500L, 3L);

        when(userGuessRepository.findAllTimeLeaderboard(any(Pageable.class))).thenReturn(List.of(row1, row2));
        when(userLookupService.findByIds(Set.of(user1Id, user2Id))).thenReturn(Map.of(
                user1Id, new UserSummaryDto(user1Id, "76561198000000001", "s1mple", "https://avatar/s1mple.jpg"),
                user2Id, new UserSummaryDto(user2Id, "76561198000000002", "ZywOo", "https://avatar/zywoo.jpg")
        ));

        LeaderboardResponseDto response = dailyChallengeService.getLeaderboard("ALL_TIME", null, 20);

        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries().get(0).rank()).isEqualTo(1);
        assertThat(response.entries().get(0).username()).isEqualTo("s1mple");
        assertThat(response.entries().get(0).score()).isEqualTo(2800L);

        assertThat(response.entries().get(1).rank()).isEqualTo(2);
        assertThat(response.entries().get(1).username()).isEqualTo("ZywOo");
    }

    private record TestLeaderboardRow(UUID userId, Long totalScore, Long roundsCount) implements LeaderboardRowProjection {
        @Override
        public UUID getUserId() {
            return userId;
        }

        @Override
        public Long getTotalScore() {
            return totalScore;
        }

        @Override
        public Long getRoundsCount() {
            return roundsCount;
        }
    }
}
