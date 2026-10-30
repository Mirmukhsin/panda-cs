package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.ThrowType;
import com.cs2hub.stratboard.domain.UtilityType;
import com.cs2hub.stratboard.dto.SubmitLineupRequestDto;
import com.cs2hub.stratboard.dto.UtilityLineupDto;
import com.cs2hub.stratboard.service.UtilityLineupService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilityLineupControllerTest {

    @Mock
    private UtilityLineupService utilityLineupService;

    private UtilityLineupController controller;

    @BeforeEach
    void setUp() {
        controller = new UtilityLineupController(utilityLineupService);
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/lineups should return list of lineups")
    void shouldGetLineups() {
        UtilityLineupDto dto = new UtilityLineupDto(
                UUID.randomUUID(), "de_mirage", UtilityType.SMOKE, StratSide.T,
                "Window Smoke", ThrowType.JUMP_THROW, 48.0, 18.0, 44.0, 46.0,
                "https://sample.video", "Aim at trash can corner", null, true,
                Instant.now()
        );

        when(utilityLineupService.getLineups("de_mirage", UtilityType.SMOKE, StratSide.T))
                .thenReturn(List.of(dto));

        ResponseEntity<ApiResponseDto<List<UtilityLineupDto>>> response =
                controller.getLineups("de_mirage", UtilityType.SMOKE, StratSide.T);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).title()).isEqualTo("Window Smoke");
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/lineups/{id} should return single lineup")
    void shouldGetLineupById() {
        UUID lineupId = UUID.randomUUID();
        UtilityLineupDto dto = new UtilityLineupDto(
                lineupId, "de_dust2", UtilityType.FLASH, StratSide.CT,
                "Long A Popflash", ThrowType.STANDING, 70.0, 80.0, 72.0, 78.0,
                "https://sample.video", null, null, true, Instant.now()
        );

        when(utilityLineupService.getLineupById(lineupId)).thenReturn(dto);

        ResponseEntity<ApiResponseDto<UtilityLineupDto>> response = controller.getLineupById(lineupId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().title()).isEqualTo("Long A Popflash");
    }

    @Test
    @DisplayName("POST /api/v1/stratboard/lineups should require authentication and create lineup")
    void shouldSubmitLineupWhenAuthenticated() {
        SubmitLineupRequestDto request = new SubmitLineupRequestDto(
                "de_ancient", UtilityType.SMOKE, StratSide.T, "Cave Smoke",
                ThrowType.STANDING, 20.0, 30.0, 40.0, 50.0, "https://youtube.com/sample", null
        );

        // Unauthenticated -> throws UnauthorizedException
        assertThatThrownBy(() -> controller.submitLineup(null, request))
                .isInstanceOf(UnauthorizedException.class);

        // Authenticated
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);
        UtilityLineupDto created = new UtilityLineupDto(
                UUID.randomUUID(), "de_ancient", UtilityType.SMOKE, StratSide.T,
                "Cave Smoke", ThrowType.STANDING, 20.0, 30.0, 40.0, 50.0,
                "https://youtube.com/sample", null, userId, false, Instant.now()
        );

        when(utilityLineupService.submitLineup(userId, request)).thenReturn(created);

        ResponseEntity<ApiResponseDto<UtilityLineupDto>> response = controller.submitLineup(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().isVerified()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/stratboard/lineups/{id} should require authentication and delete lineup")
    void shouldDeleteLineup() {
        UUID lineupId = UUID.randomUUID();

        // Unauthenticated -> throws UnauthorizedException
        assertThatThrownBy(() -> controller.deleteLineup(lineupId, null))
                .isInstanceOf(UnauthorizedException.class);

        // Authenticated
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);

        ResponseEntity<ApiResponseDto<Void>> response = controller.deleteLineup(lineupId, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(utilityLineupService).deleteLineup(lineupId, user);
    }
}
