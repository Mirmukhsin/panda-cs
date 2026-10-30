package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.WearCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed weapon item with sticker craft metadata in a saved loadout")
public record UserLoadoutItemDetailDto(
        @Schema(description = "Loadout item unique identifier")
        UUID id,

        @Schema(description = "Weapon equipment slot identifier", example = "T_RIFLE")
        String weaponSlot,

        @Schema(description = "Skin identifier", example = "skin-ak47-redline")
        String skinId,

        @Schema(description = "Skin display name", example = "AK-47 | Redline")
        String skinName,

        @Schema(description = "Weapon display name", example = "AK-47")
        String weaponName,

        @Schema(description = "Weapon wear condition", example = "FIELD_TESTED")
        WearCondition wearCondition,

        @Schema(description = "Skin price in USD for selected wear condition", example = "18.50")
        BigDecimal selectedPriceUsd,

        @Schema(description = "Primary skin color theme", example = "RED")
        SkinColor primaryColor,

        @Schema(description = "Skin image URL")
        String imageUrl,

        @Schema(description = "Applied stickers and positions")
        List<StickerPlacementDto> stickers
) implements Serializable {}
