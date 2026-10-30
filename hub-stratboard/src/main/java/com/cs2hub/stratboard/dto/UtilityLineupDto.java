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
        Instant createdAt
) implements Serializable {

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
                entity.getCreatedAt()
        );
    }
}
