package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Complete snapshot of an active collaborative tactical room")
public record StratRoomSnapshotDto(
        @Schema(description = "Unique 8-character room join code", example = "X7K9M2P4")
        String roomCode,

        @Schema(description = "Active map identifier", example = "de_mirage")
        String mapName,

        @Schema(description = "Active team side", example = "T")
        StratSide teamSide,

        @Schema(description = "Host user UUID if authenticated, null if guest")
        UUID hostUserId,

        @Schema(description = "Host participant ID", example = "part-host")
        String hostParticipantId,

        @Schema(description = "Currently active room participants")
        List<RoomParticipantDto> participants,

        @Schema(description = "Current canvas drawing elements and tokens on the board")
        List<CanvasElementDto> canvasElements,

        @Schema(description = "Room creation timestamp")
        Instant createdAt,

        @Schema(description = "Last activity timestamp")
        Instant lastActivityAt
) implements Serializable {}
