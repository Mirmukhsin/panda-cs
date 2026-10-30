package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratBoardEventType;
import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

@Schema(description = "Inbound client event published to /app/stratboard/rooms/{roomCode}/event")
public record StratBoardWsEventDto(
        @NotNull
        @Schema(description = "Event action type")
        StratBoardEventType eventType,

        @NotBlank
        @Schema(description = "Sender participant identifier", example = "part-1234")
        String participantId,

        @Schema(description = "Participant details for PARTICIPANT_JOIN / PARTICIPANT_LEAVE")
        RoomParticipantDto participant,

        @Schema(description = "Real-time cursor position for CURSOR_MOVE")
        CursorPositionDto cursor,

        @Schema(description = "Element data for ELEMENT_UPSERT")
        CanvasElementDto element,

        @Schema(description = "Target element identifier for ELEMENT_DELETE", example = "elem-9876")
        String elementId,

        @Schema(description = "New map identifier for MAP_CHANGE", example = "de_inferno")
        String mapName,

        @Schema(description = "New team side for MAP_CHANGE", example = "CT")
        StratSide teamSide,

        @Schema(description = "Tactic ID or shareCode for LOAD_TACTIC")
        String tacticIdOrShareCode,

        @Schema(description = "Serialized canvas JSON for custom state replace")
        String canvasStateJson
) implements Serializable {}
