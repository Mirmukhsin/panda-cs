package com.cs2hub.stratboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Participant real-time cursor coordinate")
public record CursorPositionDto(
        @Schema(description = "Normalized X coordinate (0.0 - 100.0)", example = "45.2")
        Double x,

        @Schema(description = "Normalized Y coordinate (0.0 - 100.0)", example = "62.8")
        Double y
) implements Serializable {}
