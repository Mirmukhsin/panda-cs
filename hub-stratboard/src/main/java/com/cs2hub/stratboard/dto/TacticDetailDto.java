package com.cs2hub.stratboard.dto;

import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Full tactic detail including serialized canvas drawing state")
public record TacticDetailDto(
        @Schema(description = "Tactic UUID")
        UUID id,

        @Schema(description = "Author user UUID")
        UUID authorUserId,

        @Schema(description = "Tactic title", example = "Mirage A-Site Fast Execute")
        String title,

        @Schema(description = "Map identifier", example = "de_mirage")
        String mapName,

        @Schema(description = "Team side", example = "T")
        StratSide teamSide,

        @Schema(description = "Tactic description / playbook notes")
        String description,

        @Schema(description = "Serialized canvas drawing state JSON")
        String canvasStateJson,

        @Schema(description = "Whether the tactic is publicly viewable")
        Boolean isPublic,

        @Schema(description = "Total community upvotes", example = "12")
        Integer upvotes,

        @Schema(description = "Short 8-character share code", example = "A7X9K2M4")
        String shareCode,

        @Schema(description = "Author user profile details")
        UserSummaryDto author,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) implements Serializable {}
