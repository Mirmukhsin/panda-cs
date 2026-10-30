package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.dto.SaveLoadoutRequestDto;
import com.cs2hub.loadouts.dto.UpvoteResponseDto;
import com.cs2hub.loadouts.dto.UserLoadoutDetailDto;
import com.cs2hub.loadouts.dto.UserLoadoutSummaryDto;
import com.cs2hub.loadouts.service.CommunityLoadoutService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityLoadoutControllerTest {

    @Mock
    private CommunityLoadoutService communityLoadoutService;

    private CommunityLoadoutController controller;

    @BeforeEach
    void setUp() {
        controller = new CommunityLoadoutController(communityLoadoutService);
    }

    @Test
    @DisplayName("GET /api/v1/loadouts/community should return 200 with feed page")
    void shouldGetPublicLoadoutsFeed() {
        UserLoadoutSummaryDto summary = new UserLoadoutSummaryDto(
                UUID.randomUUID(), UUID.randomUUID(), "Titan Blue",
                SkinColor.BLUE, new BigDecimal("100.00"), new BigDecimal("95.00"),
                10, true, 8, null, false, Instant.now(), Instant.now()
        );
        Page<UserLoadoutSummaryDto> page = new PageImpl<>(List.of(summary));

        when(communityLoadoutService.getPublicLoadouts(SkinColor.BLUE, null, "popular", 0, 20, Optional.empty()))
                .thenReturn(page);

        ResponseEntity<ApiResponseDto<Page<UserLoadoutSummaryDto>>> response = controller.getPublicLoadouts(
                SkinColor.BLUE, null, "popular", 0, 20, Optional.empty()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/community should return 201 CREATED when authenticated")
    void shouldCreateLoadoutWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "broky", "USER");

        SaveLoadoutRequestDto request = new SaveLoadoutRequestDto(
                "My Craft", SkinColor.RED, new BigDecimal("100.00"), true, Collections.emptyList()
        );

        UserLoadoutDetailDto detail = new UserLoadoutDetailDto(
                UUID.randomUUID(), userId, "My Craft", SkinColor.RED,
                new BigDecimal("100.00"), new BigDecimal("75.00"), 0, true,
                Collections.emptyList(), null, false, Instant.now(), Instant.now()
        );

        when(communityLoadoutService.createLoadout(userId, request)).thenReturn(detail);

        ResponseEntity<ApiResponseDto<UserLoadoutDetailDto>> response = controller.createLoadout(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().title()).isEqualTo("My Craft");
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/community should throw UnauthorizedException when user is null")
    void shouldRejectCreateWhenUnauthenticated() {
        SaveLoadoutRequestDto request = new SaveLoadoutRequestDto(
                "My Craft", SkinColor.RED, new BigDecimal("100.00"), true, Collections.emptyList()
        );

        assertThatThrownBy(() -> controller.createLoadout(null, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/community/{id}/upvote should return 200 with updated upvote status")
    void shouldToggleUpvoteSuccessfully() {
        UUID loadoutId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "broky", "USER");

        UpvoteResponseDto upvoteDto = new UpvoteResponseDto(loadoutId, 15, true);
        when(communityLoadoutService.toggleUpvote(loadoutId, userId)).thenReturn(upvoteDto);

        ResponseEntity<ApiResponseDto<UpvoteResponseDto>> response = controller.toggleUpvote(loadoutId, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().upvotes()).isEqualTo(15);
        assertThat(response.getBody().data().upvoted()).isTrue();
    }

    @Test
    @DisplayName("DELETE /api/v1/loadouts/community/{id} should return 200 OK")
    void shouldDeleteLoadoutSuccessfully() {
        UUID loadoutId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "broky", "USER");

        ResponseEntity<ApiResponseDto<Void>> response = controller.deleteLoadout(loadoutId, user);

        verify(communityLoadoutService).deleteLoadout(loadoutId, user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
