package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratBoardEventType;
import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Schema(description = "Outbound broadcast payload sent to subscribers of /topic/stratboard/rooms/{roomCode}")
public record StratBoardWsBroadcastDto(
        @Schema(description = "Room code", example = "X7K9M2P4")
        String roomCode,

        @Schema(description = "Type of event processed")
        StratBoardEventType eventType,

        @Schema(description = "Participant who initiated the event")
        String participantId,

        @Schema(description = "Participant details for join/leave")
        RoomParticipantDto participant,

        @Schema(description = "Cursor position for CURSOR_MOVE")
        CursorPositionDto cursor,

        @Schema(description = "Upserted element for ELEMENT_UPSERT")
        CanvasElementDto element,

        @Schema(description = "Deleted element ID for ELEMENT_DELETE")
        String elementId,

        @Schema(description = "Updated map name for MAP_CHANGE")
        String mapName,

        @Schema(description = "Updated team side for MAP_CHANGE")
        StratSide teamSide,

        @Schema(description = "Full canvas elements list for BOARD_CLEAR, LOAD_TACTIC, or batch synchronization")
        List<CanvasElementDto> canvasElements,

        @Schema(description = "Broadcast timestamp")
        Instant timestamp
) implements Serializable {

    public static StratBoardWsBroadcastDto of(
            String roomCode,
            StratBoardEventType eventType,
            String participantId
    ) {
        return new StratBoardWsBroadcastDto(
                roomCode,
                eventType,
                participantId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now()
        );
    }
}
