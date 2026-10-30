package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

@Schema(description = "Sticker placement on a weapon skin")
public record StickerPlacementDto(
        @NotBlank
        @Schema(description = "Sticker catalog identifier", example = "sticker-titan-holo-katowice-2014")
        String stickerId,

        @Min(0)
        @Max(4)
        @Schema(description = "Weapon sticker slot index (0 to 4)", example = "0")
        int slotIndex,

        @Schema(description = "Horizontal position offset", example = "0.0")
        double offsetX,

        @Schema(description = "Vertical position offset", example = "0.0")
        double offsetY,

        @Schema(description = "Rotation in degrees (-180 to 180)", example = "0.0")
        double rotation,

        @Schema(description = "Scale factor multiplier", example = "1.0")
        double scale
) implements Serializable {}
