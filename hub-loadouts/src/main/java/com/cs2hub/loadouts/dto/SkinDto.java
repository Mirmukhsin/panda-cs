package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Weapon skin catalog item")
public record SkinDto(
        @Schema(description = "Unique skin identifier", example = "skin-ak47-asiimov")
        String id,

        @Schema(description = "Full skin display name", example = "AK-47 | Asiimov")
        String name,

        @Schema(description = "Weapon technical identifier", example = "weapon_ak47")
        String weaponId,

        @Schema(description = "Weapon name", example = "AK-47")
        String weaponName,

        @Schema(description = "Weapon category", example = "RIFLES")
        WeaponCategory category,

        @Schema(description = "Playable team side", example = "T")
        TeamSide teamSide,

        @Schema(description = "Item rarity grade", example = "Covert")
        String rarity,

        @Schema(description = "Primary theme color", example = "ORANGE")
        SkinColor primaryColor,

        @Schema(description = "Secondary accent color", example = "WHITE")
        SkinColor secondaryColor,

        @Schema(description = "Skin preview image URL", example = "https://community.cloudflare.steamstatic.com/economy/image/...")
        String imageUrl,

        @Schema(description = "Minimum wear float", example = "0.05")
        Double minFloat,

        @Schema(description = "Maximum wear float", example = "0.70")
        Double maxFloat,

        @Schema(description = "Factory New (FN) price in USD", example = "150.00")
        BigDecimal priceFn,

        @Schema(description = "Minimal Wear (MW) price in USD", example = "85.00")
        BigDecimal priceMw,

        @Schema(description = "Field-Tested (FT) price in USD", example = "45.00")
        BigDecimal priceFt,

        @Schema(description = "Well-Worn (WW) price in USD", example = "35.00")
        BigDecimal priceWw,

        @Schema(description = "Battle-Scarred (BS) price in USD", example = "28.00")
        BigDecimal priceBs,

        @Schema(description = "Lowest available wear price in USD", example = "28.00")
        BigDecimal lowestPriceUsd
) implements Serializable {

    public static SkinDto fromEntity(SkinEntity entity) {
        return new SkinDto(
                entity.getId(),
                entity.getName(),
                entity.getWeaponId(),
                entity.getWeaponName(),
                entity.getCategory(),
                entity.getTeamSide(),
                entity.getRarity(),
                entity.getPrimaryColor(),
                entity.getSecondaryColor(),
                entity.getImageUrl(),
                entity.getMinFloat(),
                entity.getMaxFloat(),
                entity.getPriceFn(),
                entity.getPriceMw(),
                entity.getPriceFt(),
                entity.getPriceWw(),
                entity.getPriceBs(),
                entity.getLowestPriceUsd()
        );
    }
}
