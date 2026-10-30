package com.cs2hub.stratboard.domain;

import com.cs2hub.stratboard.dto.CanvasElementDto;
import com.cs2hub.stratboard.dto.RoomParticipantDto;
import com.cs2hub.stratboard.dto.StratRoomSnapshotDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class StratRoomState {

    private final String roomCode;

    @Setter
    private volatile String mapName;

    @Setter
    private volatile StratSide teamSide;

    private final UUID hostUserId;
    private final String hostParticipantId;

    private final Map<String, RoomParticipantDto> participants = new ConcurrentHashMap<>();
    private final Map<String, CanvasElementDto> canvasElements = new ConcurrentHashMap<>();

    private final Instant createdAt;

    @Setter
    private volatile Instant lastActivityAt;

    public StratRoomState(
            String roomCode,
            String mapName,
            StratSide teamSide,
            UUID hostUserId,
            String hostParticipantId,
            RoomParticipantDto hostParticipant
    ) {
        this.roomCode = roomCode;
        this.mapName = mapName;
        this.teamSide = teamSide;
        this.hostUserId = hostUserId;
        this.hostParticipantId = hostParticipantId;
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();

        if (hostParticipant != null) {
            this.participants.put(hostParticipant.participantId(), hostParticipant);
        }
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public StratRoomSnapshotDto toSnapshotDto() {
        return new StratRoomSnapshotDto(
                roomCode,
                mapName,
                teamSide,
                hostUserId,
                hostParticipantId,
                new ArrayList<>(participants.values()),
                new ArrayList<>(canvasElements.values()),
                createdAt,
                lastActivityAt
        );
    }
}
