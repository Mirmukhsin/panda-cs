package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Generated color-matched budget loadout response")
public record GeneratedLoadoutResponseDto(
        @Schema(description = "Primary loadout color theme", example = "RED")
        SkinColor themeColor,

        @Schema(description = "Maximum budget limit in USD", example = "50.00")
        BigDecimal budgetLimitUsd,

        @Schema(description = "Total cost of all selected weapon skins in USD", example = "48.20")
        BigDecimal totalCostUsd,

        @Schema(description = "Unspent remaining budget in USD", example = "1.80")
        BigDecimal remainingBudgetUsd,

        @Schema(description = "Generated weapon items list")
        List<GeneratedLoadoutItemDto> items
) implements Serializable {}
