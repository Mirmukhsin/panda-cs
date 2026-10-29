package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.ReviewClipRequestDto;
import com.cs2hub.minigames.service.ClipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClipControllerTest {

    @Mock
    private ClipService clipService;

    private AdminClipController adminClipController;

    @BeforeEach
    void setUp() {
        adminClipController = new AdminClipController(clipService);
    }

    @Test
    @DisplayName("GET /api/v1/minigames/admin/clips should succeed for ADMIN user")
    void shouldListClipsForAdmin() {
        AuthenticatedUser adminUser = new AuthenticatedUser(
                UUID.randomUUID(), "76561198000000999", "admin_lead", AuthenticatedUser.ROLE_ADMIN
        );

        ClipSubmissionResponseDto pendingClip = new ClipSubmissionResponseDto(
                UUID.randomUUID(), "Dust2 long cross", "https://youtube.com/watch?v=d2",
                VideoProvider.YOUTUBE, "de_dust2", 14000, 6, ClipStatus.PENDING,
                new UserSummaryDto(UUID.randomUUID(), "76561198000000001", "submitter", null),
                null, Instant.now(), Instant.now()
        );

        when(clipService.getClipsForModeration(ClipStatus.PENDING)).thenReturn(List.of(pendingClip));

        ResponseEntity<ApiResponseDto<List<ClipSubmissionResponseDto>>> response =
                adminClipController.listClipsForReview(adminUser, ClipStatus.PENDING);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/minigames/admin/clips should throw AccessDeniedException (403) for non-admin user")
    void shouldDenyAccessToNonAdminUser() {
        AuthenticatedUser regularUser = new AuthenticatedUser(
                UUID.randomUUID(), "76561198000000002", "regular_joe", AuthenticatedUser.ROLE_USER
        );

        assertThatThrownBy(() -> adminClipController.listClipsForReview(regularUser, ClipStatus.PENDING))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Administrative privileges required");
    }

    @Test
    @DisplayName("PATCH /api/v1/minigames/admin/clips/{id}/review should succeed for ADMIN user")
    void shouldReviewClipForAdmin() {
        UUID clipId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser adminUser = new AuthenticatedUser(adminId, "76561198000000999", "admin_lead", "ADMIN");

        ReviewClipRequestDto request = new ReviewClipRequestDto(ClipStatus.APPROVED, 16000);

        ClipSubmissionResponseDto reviewed = new ClipSubmissionResponseDto(
                clipId, "Mirage Palace 1v2", "https://youtube.com/watch?v=mirage",
                VideoProvider.YOUTUBE, "de_mirage", 16000, 7, ClipStatus.APPROVED,
                new UserSummaryDto(UUID.randomUUID(), "76561198000000001", "submitter", null),
                adminId, Instant.now(), Instant.now()
        );

        when(clipService.reviewClip(clipId, adminUser, request)).thenReturn(reviewed);

        ResponseEntity<ApiResponseDto<ClipSubmissionResponseDto>> response =
                adminClipController.reviewClip(adminUser, clipId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo(ClipStatus.APPROVED);
        assertThat(response.getBody().data().reviewedByUserId()).isEqualTo(adminId);
    }

    @Test
    @DisplayName("PATCH /api/v1/minigames/admin/clips/{id}/review should throw AccessDeniedException (403) for non-admin user")
    void shouldDenyReviewToNonAdminUser() {
        UUID clipId = UUID.randomUUID();
        AuthenticatedUser regularUser = new AuthenticatedUser(
                UUID.randomUUID(), "76561198000000002", "regular_joe", "USER"
        );
        ReviewClipRequestDto request = new ReviewClipRequestDto(ClipStatus.APPROVED, null);

        assertThatThrownBy(() -> adminClipController.reviewClip(regularUser, clipId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Administrative privileges required");
    }
}
