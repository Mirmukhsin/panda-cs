package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.dto.SaveTacticRequestDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.cs2hub.stratboard.dto.TacticSummaryDto;
import com.cs2hub.stratboard.service.TacticService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacticControllerTest {

    @Mock
    private TacticService tacticService;

    private TacticController controller;

    @BeforeEach
    void setUp() {
        controller = new TacticController(tacticService);
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/tactics should return public tactics page")
    void shouldGetPublicTactics() {
        TacticSummaryDto summary = new TacticSummaryDto(
                UUID.randomUUID(), UUID.randomUUID(), "Mirage Fast A", "de_mirage", StratSide.T,
                "Execute description", true, 15, "FAST1234", null, Instant.now(), Instant.now()
        );
        Page<TacticSummaryDto> page = new PageImpl<>(List.of(summary));

        when(tacticService.getPublicTactics("de_mirage", StratSide.T, 0, 20)).thenReturn(page);

        ResponseEntity<ApiResponseDto<Page<TacticSummaryDto>>> response =
                controller.getPublicTactics("de_mirage", StratSide.T, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
        assertThat(response.getBody().data().getContent().get(0).title()).isEqualTo("Mirage Fast A");
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/tactics/my should require authentication and return user tactics")
    void shouldGetMyTactics() {
        assertThatThrownBy(() -> controller.getMyTactics(null))
                .isInstanceOf(UnauthorizedException.class);

        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);

        TacticSummaryDto summary = new TacticSummaryDto(
                UUID.randomUUID(), userId, "My Strat", "de_inferno", StratSide.CT,
                null, true, 0, "MYST1234", null, Instant.now(), Instant.now()
        );

        when(tacticService.getUserTactics(userId)).thenReturn(List.of(summary));

        ResponseEntity<ApiResponseDto<List<TacticSummaryDto>>> response = controller.getMyTactics(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/tactics/{idOrShareCode} should return tactic detail")
    void shouldGetTacticByIdOrShareCode() {
        String shareCode = "7K9X2B4F";
        TacticDetailDto detail = new TacticDetailDto(
                UUID.randomUUID(), UUID.randomUUID(), "Dust2 Mid to B", "de_dust2", StratSide.T,
                "Detailed notes", "{\"version\":1}", true, 42, shareCode, null, Instant.now(), Instant.now()
        );

        when(tacticService.getTacticByIdOrShareCode(shareCode, Optional.empty())).thenReturn(detail);

        ResponseEntity<ApiResponseDto<TacticDetailDto>> response =
                controller.getTacticByIdOrShareCode(shareCode, Optional.empty());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareCode()).isEqualTo(shareCode);
    }

    @Test
    @DisplayName("POST /api/v1/stratboard/tactics should require authentication and return 201 Created")
    void shouldCreateTactic() {
        SaveTacticRequestDto request = new SaveTacticRequestDto(
                "Anubis B Split", "de_anubis", StratSide.T, "Description", "{\"data\":1}", true
        );

        assertThatThrownBy(() -> controller.createTactic(null, request))
                .isInstanceOf(UnauthorizedException.class);

        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);
        TacticDetailDto created = new TacticDetailDto(
                UUID.randomUUID(), userId, "Anubis B Split", "de_anubis", StratSide.T,
                "Description", "{\"data\":1}", true, 0, "ANUB1234", null, Instant.now(), Instant.now()
        );

        when(tacticService.createTactic(userId, request)).thenReturn(created);

        ResponseEntity<ApiResponseDto<TacticDetailDto>> response = controller.createTactic(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareCode()).isEqualTo("ANUB1234");
    }

    @Test
    @DisplayName("PUT /api/v1/stratboard/tactics/{id} should require authentication and update tactic")
    void shouldUpdateTactic() {
        UUID tacticId = UUID.randomUUID();
        SaveTacticRequestDto request = new SaveTacticRequestDto(
                "Updated", "de_anubis", StratSide.T, "Desc", "{\"data\":2}", true
        );

        assertThatThrownBy(() -> controller.updateTactic(tacticId, null, request))
                .isInstanceOf(UnauthorizedException.class);

        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);
        TacticDetailDto updated = new TacticDetailDto(
                tacticId, user.userId(), "Updated", "de_anubis", StratSide.T,
                "Desc", "{\"data\":2}", true, 5, "ANUB1234", null, Instant.now(), Instant.now()
        );

        when(tacticService.updateTactic(tacticId, user, request)).thenReturn(updated);

        ResponseEntity<ApiResponseDto<TacticDetailDto>> response = controller.updateTactic(tacticId, user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().title()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("DELETE /api/v1/stratboard/tactics/{id} should require authentication and delete tactic")
    void shouldDeleteTactic() {
        UUID tacticId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.deleteTactic(tacticId, null))
                .isInstanceOf(UnauthorizedException.class);

        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);

        ResponseEntity<ApiResponseDto<Void>> response = controller.deleteTactic(tacticId, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tacticService).deleteTactic(tacticId, user);
    }
}
