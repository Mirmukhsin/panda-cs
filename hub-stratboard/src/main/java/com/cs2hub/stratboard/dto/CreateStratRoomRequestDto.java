package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Schema(description = "Request body to initialize a new real-time collaborative tactical room")
public record CreateStratRoomRequestDto(
        @NotBlank
        @Schema(description = "Map identifier (e.g. de_mirage, de_inferno, de_dust2)", example = "de_mirage")
        String mapName,

        @NotNull
        @Schema(description = "Team side", example = "T")
        StratSide teamSide,

        @NotBlank
        @Size(max = 64)
        @Schema(description = "Host display name", example = "IGL")
        String hostDisplayName,

        @Schema(description = "Host assigned color hex", example = "#3B82F6")
        String hostColorHex,

        @Schema(description = "Optional tactic ID or 8-char shareCode to pre-load into the room")
        String initialTacticIdOrShareCode
) implements Serializable {}
