package com.cs2hub.stratboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Schema(description = "Request body to snapshot and persist a collaborative room into a saved tactic playbook")
public record SaveRoomTacticRequestDto(
        @NotBlank
        @Size(max = 160)
        @Schema(description = "Tactic title", example = "Mirage Fast A Execution")
        String title,

        @Schema(description = "Tactic playbook notes and instructions", example = "Smokes land at 1:45, execute through palace and ramp")
        String description,

        @Schema(description = "Whether the tactic will be publicly visible", example = "true")
        Boolean isPublic
) implements Serializable {}
