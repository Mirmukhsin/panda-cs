package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request body to save a community loadout and sticker craft")
public record SaveLoadoutRequestDto(
        @NotBlank
        @Size(max = 160)
        @Schema(description = "Loadout title", example = "Crimson Strike - Full Red Themed Loadout")
        String title,

        @Schema(description = "Overall color theme", example = "RED")
        SkinColor themeColor,

        @Schema(description = "Optional budget ceiling in USD", example = "150.00")
        BigDecimal budgetLimitUsd,

        @Schema(description = "Whether the loadout is publicly listed in community feed", example = "true")
        Boolean isPublic,

        @NotEmpty
        @Valid
        @Schema(description = "List of weapon skin items in the loadout")
        List<SaveLoadoutItemDto> items
) implements Serializable {}
