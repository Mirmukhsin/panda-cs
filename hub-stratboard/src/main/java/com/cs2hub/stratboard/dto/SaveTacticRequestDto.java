package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Schema(description = "Request body to save or update a team tactic / playbook")
public record SaveTacticRequestDto(
        @NotBlank
        @Size(max = 160)
        @Schema(description = "Tactic title", example = "Mirage A-Site Fast Execute")
        String title,

        @NotBlank
        @Schema(description = "Map identifier", example = "de_mirage")
        String mapName,

        @NotNull
        @Schema(description = "Team side executing the tactic", example = "T")
        StratSide teamSide,

        @Schema(description = "Playbook description or round callout notes", example = "Execute at 1:20 after default map control")
        String description,

        @NotBlank
        @Schema(description = "Serialized canvas drawing state JSON (paths, player tokens, utility markers, text labels)")
        String canvasStateJson,

        @Schema(description = "Whether the tactic is publicly viewable in community playbooks feed", example = "true")
        Boolean isPublic
) implements Serializable {}
