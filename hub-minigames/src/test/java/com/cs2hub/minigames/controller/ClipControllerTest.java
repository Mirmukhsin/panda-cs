package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.SubmitClipRequestDto;
import com.cs2hub.minigames.service.ClipService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClipControllerTest {

    @Mock
    private ClipService clipService;

    private ClipController clipController;

    @BeforeEach
    void setUp() {
        clipController = new ClipController(clipService);
    }

    @Test
    @DisplayName("POST /api/v1/minigames/clips should create clip and return 201 CREATED")
    void shouldSubmitClipSuccessfully() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "broky", "USER");
        SubmitClipRequestDto request = new SubmitClipRequestDto(
                "Anubis mid pick", "https://youtube.com/watch?v=anubis", "de_anubis", 19000, 8
        );

        ClipSubmissionResponseDto expectedResponse = new ClipSubmissionResponseDto(
                UUID.randomUUID(), "Anubis mid pick", "https://youtube.com/watch?v=anubis",
                VideoProvider.YOUTUBE, "de_anubis", 19000, 8, ClipStatus.PENDING,
                new UserSummaryDto(userId, "76561198000000001", "broky", null),
                null, Instant.now(), Instant.now()
        );

        when(clipService.submitClip(user, request)).thenReturn(expectedResponse);

        ResponseEntity<ApiResponseDto<ClipSubmissionResponseDto>> response = clipController.submitClip(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().title()).isEqualTo("Anubis mid pick");
    }

    @Test
    @DisplayName("POST /api/v1/minigames/clips should throw UnauthorizedException for guests")
    void shouldRejectUnauthenticatedSubmission() {
        SubmitClipRequestDto request = new SubmitClipRequestDto(
                "Test", "https://youtube.com/watch?v=test", "de_dust2", 15000, null
        );

        assertThatThrownBy(() -> clipController.submitClip(null, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("GET /api/v1/minigames/clips/my should return clips for current user")
    void shouldReturnMyClips() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "rain", "USER");

        ClipSubmissionResponseDto clip = new ClipSubmissionResponseDto(
                UUID.randomUUID(), "Rain ace", "https://youtube.com/watch?v=rain",
                VideoProvider.YOUTUBE, "de_nuke", 24000, 10, ClipStatus.APPROVED,
                new UserSummaryDto(userId, "76561198000000001", "rain", null),
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(clipService.getMyClips(user)).thenReturn(List.of(clip));

        ResponseEntity<ApiResponseDto<List<ClipSubmissionResponseDto>>> response = clipController.getMyClips(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
    }
}
