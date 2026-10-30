package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.CanvasElementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Individual drawn element or token on the interactive tactical whiteboard")
public record CanvasElementDto(
        @Schema(description = "Unique element identifier", example = "elem-9876")
        String elementId,

        @Schema(description = "Element category", example = "ARROW")
        CanvasElementType elementType,

        @Schema(description = "Primary or starting X coordinate (0.0 - 100.0)", example = "35.5")
        Double x,

        @Schema(description = "Primary or starting Y coordinate (0.0 - 100.0)", example = "42.0")
        Double y,

        @Schema(description = "Ending X coordinate for line/arrow elements", example = "55.0")
        Double endX,

        @Schema(description = "Ending Y coordinate for line/arrow elements", example = "60.0")
        Double endY,

        @Schema(description = "Coordinate point sequence for freehand drawings")
        List<Map<String, Double>> points,

        @Schema(description = "Hex stroke or fill color", example = "#EF4444")
        String colorHex,

        @Schema(description = "Stroke line thickness in pixels", example = "3.0")
        Double strokeWidth,

        @Schema(description = "Text content for text labels", example = "Smoke here at 1:40")
        String text,

        @Schema(description = "Short token badge/label (e.g. 1, 2, AWP, IGL)", example = "AWP")
        String label,

        @Schema(description = "Linked utility lineup UUID if pinned from library")
        UUID lineupId,

        @Schema(description = "Identifier of the participant who created or last modified this element")
        String createdByParticipantId,

        @Schema(description = "Timestamp when element was last updated")
        Instant updatedAt
) implements Serializable {}
