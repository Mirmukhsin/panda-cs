package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Request parameters to generate a color-matched budget loadout")
public record GenerateLoadoutRequestDto(
        @NotNull
        @DecimalMin(value = "5.00", message = "Budget must be at least $5.00")
        @Schema(description = "Target total budget in USD", example = "50.00")
        BigDecimal budgetUsd,

        @NotNull
        @Schema(description = "Primary color theme", example = "RED")
        SkinColor themeColor,

        @Schema(description = "Preferred CT Rifle weaponId ('weapon_m4a1_silencer' or 'weapon_m4a4')", example = "weapon_m4a1_silencer")
        String preferredCtRifle,

        @Schema(description = "Whether to attempt including luxury knife and gloves slots if budget allows", example = "false")
        Boolean includeKnifeAndGloves
) implements Serializable {}
