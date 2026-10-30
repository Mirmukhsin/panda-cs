package com.cs2hub.stratboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema(description = "Participant in an active collaborative tactical room")
public record RoomParticipantDto(
        @Schema(description = "Unique session participant identifier", example = "part-1234")
        String participantId,

        @Schema(description = "Display name shown above cursor and on roster", example = "s1mple")
        String displayName,

        @Schema(description = "Hex color code assigned to the participant's drawings and cursor", example = "#3B82F6")
        String colorHex,

        @Schema(description = "User account UUID if authenticated, null for guest", example = "d94b0d1e-...")
        UUID userId,

        @Schema(description = "Whether this participant is the room host / creator")
        Boolean isHost
) implements Serializable {}
