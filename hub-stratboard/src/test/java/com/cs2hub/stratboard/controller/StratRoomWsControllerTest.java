package com.cs2hub.stratboard.controller;

import com.cs2hub.stratboard.domain.StratBoardEventType;
import com.cs2hub.stratboard.dto.CursorPositionDto;
import com.cs2hub.stratboard.dto.StratBoardWsBroadcastDto;
import com.cs2hub.stratboard.dto.StratBoardWsEventDto;
import com.cs2hub.stratboard.service.StratRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StratRoomWsControllerTest {

    @Mock
    private StratRoomService stratRoomService;

    private StratRoomWsController controller;

    @BeforeEach
    void setUp() {
        controller = new StratRoomWsController(stratRoomService);
    }

    @Test
    @DisplayName("handleRoomEvent should delegate to StratRoomService and return broadcast DTO")
    void shouldDelegateRoomEvent() {
        String roomCode = "X7K9M2P4";
        StratBoardWsEventDto event = new StratBoardWsEventDto(
                StratBoardEventType.CURSOR_MOVE, "part-1", null, new CursorPositionDto(10.0, 20.0),
                null, null, null, null, null, null
        );

        StratBoardWsBroadcastDto expectedBroadcast = new StratBoardWsBroadcastDto(
                roomCode, StratBoardEventType.CURSOR_MOVE, "part-1", null,
                new CursorPositionDto(10.0, 20.0), null, null, null, null, null, Instant.now()
        );

        when(stratRoomService.applyRoomEvent(roomCode, event)).thenReturn(expectedBroadcast);

        StratBoardWsBroadcastDto result = controller.handleRoomEvent(roomCode, event);

        assertThat(result).isNotNull();
        assertThat(result.roomCode()).isEqualTo(roomCode);
        assertThat(result.eventType()).isEqualTo(StratBoardEventType.CURSOR_MOVE);
        verify(stratRoomService).applyRoomEvent(roomCode, event);
    }
}
