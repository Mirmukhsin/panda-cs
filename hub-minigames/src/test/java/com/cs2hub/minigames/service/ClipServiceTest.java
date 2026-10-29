package com.cs2hub.minigames.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.ReviewClipRequestDto;
import com.cs2hub.minigames.dto.SubmitClipRequestDto;
import com.cs2hub.minigames.repository.ClipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClipServiceTest {

    @Mock
    private ClipRepository clipRepository;

    @Mock
    private UserLookupService userLookupService;

    private ClipService clipService;

    @BeforeEach
    void setUp() {
        clipService = new ClipService(clipRepository, userLookupService);
    }

    @Test
    @DisplayName("Should submit a clip, detect VideoProvider and enrich submitter info")
    void shouldSubmitClipSuccessfully() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "shox", "USER");

        SubmitClipRequestDto request = new SubmitClipRequestDto(
                "Insane 1v4 Inferno Clutch",
                "https://www.youtube.com/watch?v=sample123",
                "de_inferno",
                18500,
                8
        );

        UserSummaryDto submitterSummary = new UserSummaryDto(
                userId, "76561198000000001", "shox", "https://avatar.url/shox.jpg"
        );

        when(clipRepository.save(any(ClipEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userLookupService.findById(userId)).thenReturn(Optional.of(submitterSummary));

        ClipSubmissionResponseDto response = clipService.submitClip(user, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Insane 1v4 Inferno Clutch");
        assertThat(response.videoProvider()).isEqualTo(VideoProvider.YOUTUBE);
        assertThat(response.mapName()).isEqualTo("de_inferno");
        assertThat(response.actualPremierRating()).isEqualTo(18500);
        assertThat(response.actualFaceitLevel()).isEqualTo(8);
        assertThat(response.status()).isEqualTo(ClipStatus.PENDING);
        assertThat(response.submittedBy()).isNotNull();
        assertThat(response.submittedBy().username()).isEqualTo("shox");

        ArgumentCaptor<ClipEntity> captor = ArgumentCaptor.forClass(ClipEntity.class);
        verify(clipRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmittedByUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should fetch clips submitted by current user enriched with user summary")
    void shouldGetMyClips() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "ropz", "USER");

        ClipEntity clip = ClipEntity.builder()
                .id(UUID.randomUUID())
                .submittedByUserId(userId)
                .title("Nuke ramp hold")
                .videoUrl("https://medal.tv/clips/test")
                .videoProvider(VideoProvider.MEDAL)
                .mapName("de_nuke")
                .actualPremierRating(21000)
                .status(ClipStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(clipRepository.findAllBySubmittedByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(clip));
        when(userLookupService.findById(userId)).thenReturn(Optional.of(new UserSummaryDto(userId, "76561198000000001", "ropz", null)));

        List<ClipSubmissionResponseDto> clips = clipService.getMyClips(user);

        assertThat(clips).hasSize(1);
        assertThat(clips.get(0).title()).isEqualTo("Nuke ramp hold");
        assertThat(clips.get(0).videoProvider()).isEqualTo(VideoProvider.MEDAL);
        assertThat(clips.get(0).submittedBy().username()).isEqualTo("ropz");
    }

    @Test
    @DisplayName("Should batch lookup submitters via UserLookupService when listing clips for moderation")
    void shouldGetClipsForModerationWithBatchLookup() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        ClipEntity clip1 = ClipEntity.builder()
                .id(UUID.randomUUID())
                .submittedByUserId(user1Id)
                .title("Clip 1")
                .videoUrl("https://youtube.com/watch?v=1")
                .videoProvider(VideoProvider.YOUTUBE)
                .mapName("de_dust2")
                .actualPremierRating(12000)
                .status(ClipStatus.PENDING)
                .build();

        ClipEntity clip2 = ClipEntity.builder()
                .id(UUID.randomUUID())
                .submittedByUserId(user2Id)
                .title("Clip 2")
                .videoUrl("https://twitch.tv/clip2")
                .videoProvider(VideoProvider.TWITCH)
                .mapName("de_ancient")
                .actualPremierRating(19000)
                .status(ClipStatus.PENDING)
                .build();

        when(clipRepository.findAllByStatusOrderByCreatedAtDesc(ClipStatus.PENDING)).thenReturn(List.of(clip1, clip2));
        when(userLookupService.findByIds(Set.of(user1Id, user2Id))).thenReturn(Map.of(
                user1Id, new UserSummaryDto(user1Id, "76561198000000001", "PlayerOne", null),
                user2Id, new UserSummaryDto(user2Id, "76561198000000002", "PlayerTwo", null)
        ));

        List<ClipSubmissionResponseDto> results = clipService.getClipsForModeration(ClipStatus.PENDING);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).submittedBy().username()).isEqualTo("PlayerOne");
        assertThat(results.get(1).submittedBy().username()).isEqualTo("PlayerTwo");
    }

    @Test
    @DisplayName("Should review clip, update status and allow rating override")
    void shouldReviewClipSuccessfully() {
        UUID clipId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID submitterId = UUID.randomUUID();

        AuthenticatedUser admin = new AuthenticatedUser(adminId, "76561198000000999", "admin_guy", "ADMIN");

        ClipEntity clip = ClipEntity.builder()
                .id(clipId)
                .submittedByUserId(submitterId)
                .title("Vertigo B rush")
                .videoUrl("https://youtube.com/watch?v=vertigo")
                .videoProvider(VideoProvider.YOUTUBE)
                .mapName("de_vertigo")
                .actualPremierRating(15000)
                .status(ClipStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ReviewClipRequestDto reviewRequest = new ReviewClipRequestDto(ClipStatus.APPROVED, 15500);

        when(clipRepository.findById(clipId)).thenReturn(Optional.of(clip));
        when(clipRepository.save(any(ClipEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ClipSubmissionResponseDto reviewed = clipService.reviewClip(clipId, admin, reviewRequest);

        assertThat(reviewed.status()).isEqualTo(ClipStatus.APPROVED);
        assertThat(reviewed.reviewedByUserId()).isEqualTo(adminId);
        assertThat(reviewed.actualPremierRating()).isEqualTo(15500);
    }

    @Test
    @DisplayName("Should throw BadRequestException if review status is not APPROVED or REJECTED")
    void shouldRejectInvalidReviewStatus() {
        UUID clipId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "76561198000000999", "admin", "ADMIN");
        ReviewClipRequestDto request = new ReviewClipRequestDto(ClipStatus.PENDING, null);

        assertThatThrownBy(() -> clipService.reviewClip(clipId, admin, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be either APPROVED or REJECTED");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if clip does not exist")
    void shouldThrowNotFoundOnUnknownClip() {
        UUID unknownId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "76561198000000999", "admin", "ADMIN");
        ReviewClipRequestDto request = new ReviewClipRequestDto(ClipStatus.APPROVED, null);

        when(clipRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clipService.reviewClip(unknownId, admin, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
