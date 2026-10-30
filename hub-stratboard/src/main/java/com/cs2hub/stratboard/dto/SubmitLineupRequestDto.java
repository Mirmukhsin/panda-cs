package com.cs2hub.stratboard.dto;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.ThrowType;
import com.cs2hub.stratboard.domain.UtilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Schema(description = "Submission request for a community utility lineup")
public record SubmitLineupRequestDto(
        @NotBlank
        @Schema(description = "Map identifier (e.g. de_mirage, de_inferno, de_dust2)", example = "de_mirage")
        String mapName,

        @NotNull
        @Schema(description = "Grenade type", example = "SMOKE")
        UtilityType utilityType,

        @NotNull
        @Schema(description = "Team side", example = "T")
        StratSide teamSide,

        @NotBlank
        @Size(max = 160)
        @Schema(description = "Lineup title", example = "Mirage Window Smoke from T Spawn")
        String title,

        @NotNull
        @Schema(description = "Throw technique", example = "JUMP_THROW")
        ThrowType throwType,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Schema(description = "Standing position normalized X (0.0 - 100.0)", example = "35.5")
        Double standX,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Schema(description = "Standing position normalized Y (0.0 - 100.0)", example = "78.2")
        Double standY,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Schema(description = "Landing position normalized X (0.0 - 100.0)", example = "48.0")
        Double landX,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Schema(description = "Landing position normalized Y (0.0 - 100.0)", example = "45.0")
        Double landY,

        @NotBlank
        @Schema(description = "Lineup video or gif demonstration URL", example = "https://youtube.com/watch?v=sample")
        String videoUrl,

        @Schema(description = "Detailed lineup instructions", example = "Aim at the antenna top right, D+Jumpthrow")
        String description
) implements Serializable {}
