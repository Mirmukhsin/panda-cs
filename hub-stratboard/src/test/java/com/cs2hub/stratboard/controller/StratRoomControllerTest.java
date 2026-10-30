package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.dto.CreateStratRoomRequestDto;
import com.cs2hub.stratboard.dto.RoomParticipantDto;
import com.cs2hub.stratboard.dto.SaveRoomTacticRequestDto;
import com.cs2hub.stratboard.dto.StratRoomSnapshotDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.cs2hub.stratboard.service.StratRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
class StratRoomControllerTest {

    @Mock
    private StratRoomService stratRoomService;

    private StratRoomController controller;

    @BeforeEach
    void setUp() {
        controller = new StratRoomController(stratRoomService);
    }

    @Test
    @DisplayName("POST /api/v1/stratboard/rooms should return 201 Created with snapshot")
    void shouldCreateRoom() {
        CreateStratRoomRequestDto request = new CreateStratRoomRequestDto(
                "de_mirage", StratSide.T, "s1mple", "#3B82F6", null
        );

        StratRoomSnapshotDto snapshot = new StratRoomSnapshotDto(
                "X7K9M2P4", "de_mirage", StratSide.T, null, "part-host",
                List.of(new RoomParticipantDto("part-host", "s1mple", "#3B82F6", null, true)),
                Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(stratRoomService.createRoom(request, Optional.empty())).thenReturn(snapshot);

        ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> response = controller.createRoom(Optional.empty(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().roomCode()).isEqualTo("X7K9M2P4");
    }

    @Test
    @DisplayName("GET /api/v1/stratboard/rooms/{roomCode} should return 200 with room snapshot")
    void shouldGetRoomSnapshot() {
        String roomCode = "X7K9M2P4";
        StratRoomSnapshotDto snapshot = new StratRoomSnapshotDto(
                roomCode, "de_mirage", StratSide.T, null, "part-host",
                Collections.emptyList(), Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(stratRoomService.getRoomSnapshot(roomCode)).thenReturn(snapshot);

        ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> response = controller.getRoomSnapshot(roomCode);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().roomCode()).isEqualTo(roomCode);
    }

    @Test
    @DisplayName("POST /api/v1/stratboard/rooms/{roomCode}/load-tactic/{id} should return 200 with updated snapshot")
    void shouldLoadTacticIntoRoom() {
        String roomCode = "X7K9M2P4";
        String tacticCode = "FAST39AK";
        StratRoomSnapshotDto snapshot = new StratRoomSnapshotDto(
                roomCode, "de_mirage", StratSide.T, null, "part-host",
                Collections.emptyList(), Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(stratRoomService.loadTacticIntoRoom(roomCode, tacticCode, Optional.empty())).thenReturn(snapshot);

        ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> response =
                controller.loadTactic(roomCode, tacticCode, Optional.empty());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().roomCode()).isEqualTo(roomCode);
    }

    @Test
    @DisplayName("POST /api/v1/stratboard/rooms/{roomCode}/save-tactic should require authentication")
    void shouldSaveRoomAsTacticWhenAuthenticated() {
        String roomCode = "X7K9M2P4";
        SaveRoomTacticRequestDto request = new SaveRoomTacticRequestDto("Title", "Notes", true);

        // Unauthenticated -> throws UnauthorizedException
        assertThatThrownBy(() -> controller.saveRoomAsTactic(roomCode, null, request))
                .isInstanceOf(UnauthorizedException.class);

        // Authenticated
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "76561198000000001", "Player", AuthenticatedUser.ROLE_USER);
        TacticDetailDto savedTactic = new TacticDetailDto(
                UUID.randomUUID(), userId, "Title", "de_mirage", StratSide.T,
                "Notes", "{}", true, 0, "SAVE1234", null, Instant.now(), Instant.now()
        );

        when(stratRoomService.saveRoomAsTactic(roomCode, user, request)).thenReturn(savedTactic);

        ResponseEntity<ApiResponseDto<TacticDetailDto>> response =
                controller.saveRoomAsTactic(roomCode, user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().shareCode()).isEqualTo("SAVE1234");
    }
}
