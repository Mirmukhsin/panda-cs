package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.ThrowType;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "CS2 utility grenade lineup")
public record UtilityLineupDto(
        @Schema(description = "Lineup UUID")
        UUID id,

        @Schema(description = "Technical map identifier", example = "de_mirage")
        String mapName,

        @Schema(description = "Grenade type", example = "SMOKE")
        UtilityType utilityType,

        @Schema(description = "Executing team side", example = "T")
        StratSide teamSide,

        @Schema(description = "Lineup title", example = "Mirage Window Smoke from T Spawn")
        String title,

        @Schema(description = "Throw execution technique", example = "JUMP_THROW")
        ThrowType throwType,

        @Schema(description = "Standing position X coordinate (0.0 - 100.0)", example = "35.5")
        Double standX,

        @Schema(description = "Standing position Y coordinate (0.0 - 100.0)", example = "78.2")
        Double standY,

        @Schema(description = "Landing position X coordinate (0.0 - 100.0)", example = "48.0")
        Double landX,

        @Schema(description = "Landing position Y coordinate (0.0 - 100.0)", example = "45.0")
        Double landY,

        @Schema(description = "Short video clip demonstration URL")
        String videoUrl,

        @Schema(description = "Detailed lineup alignment and execution instructions")
        String description,

        @Schema(description = "Creator user UUID if community submitted, null if official seed")
        UUID createdByUserId,

        @Schema(description = "Whether lineup has been verified by platform administrators")
        Boolean isVerified,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        // CSNADES.GG Enhanced Specification Fields
        @Schema(description = "Origin throw callout", example = "T Spawn")
        String fromCallout,

        @Schema(description = "Target landing callout", example = "Window")
        String toCallout,

        @Schema(description = "Player movement state", example = "Stationary")
        String movement,

        @Schema(description = "Input technique description", example = "Jumpthrow")
        String technique,

        @Schema(description = "Lineup precision rating", example = "Precise")
        String precision,

        @Schema(description = "Lineup difficulty level", example = "Medium")
        String difficulty,

        @Schema(description = "Whether lineup is actively used by Tier 1 Pro players")
        Boolean isPro,

        @Schema(description = "Community view count", example = "8833")
        Integer viewsCount,

        @Schema(description = "CS2 console setpos practice command", example = "setpos -1024.00 -2450.00 -167.90; setang -32.45 145.10 0.00")
        String setposCommand,

        @Schema(description = "Estimated in-flight air time in seconds", example = "4.8")
        Double airTimeSeconds
) implements Serializable {

    // Overloaded backward-compatible constructor for existing callers and test fixtures
    public UtilityLineupDto(
            UUID id, String mapName, UtilityType utilityType, StratSide teamSide,
            String title, ThrowType throwType, Double standX, Double standY,
            Double landX, Double landY, String videoUrl, String description,
            UUID createdByUserId, Boolean isVerified, Instant createdAt
    ) {
        this(
                id, mapName, utilityType, teamSide, title, throwType,
                standX, standY, landX, landY, videoUrl, description,
                createdByUserId, isVerified, createdAt,
                null, null, "Stationary", "Left Click", "Precise",
                "Medium", false, 1200, null, 4.5
        );
    }

    public static UtilityLineupDto fromEntity(UtilityLineupEntity entity) {
        return new UtilityLineupDto(
                entity.getId(),
                entity.getMapName(),
                entity.getUtilityType(),
                entity.getTeamSide(),
                entity.getTitle(),
                entity.getThrowType(),
                entity.getStandX(),
                entity.getStandY(),
                entity.getLandX(),
                entity.getLandY(),
                entity.getVideoUrl(),
                entity.getDescription(),
                entity.getCreatedByUserId(),
                entity.getIsVerified(),
                entity.getCreatedAt(),
                entity.getFromCallout(),
                entity.getToCallout(),
                entity.getMovement(),
                entity.getTechnique(),
                entity.getPrecision(),
                entity.getDifficulty(),
                entity.getIsPro(),
                entity.getViewsCount(),
                entity.getSetposCommand(),
                entity.getAirTimeSeconds()
        );
    }
}
