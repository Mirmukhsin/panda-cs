package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.WeaponSlot;
import com.cs2hub.loadouts.domain.WearCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Single weapon item in an algorithmic budget loadout")
public record GeneratedLoadoutItemDto(
        @Schema(description = "Weapon equipment slot", example = "T_RIFLE")
        WeaponSlot slot,

        @Schema(description = "Weapon technical identifier", example = "weapon_ak47")
        String weaponId,

        @Schema(description = "Weapon display name", example = "AK-47")
        String weaponName,

        @Schema(description = "Skin identifier", example = "skin-ak47-redline")
        String skinId,

        @Schema(description = "Skin display name", example = "AK-47 | Redline")
        String skinName,

        @Schema(description = "Skin rarity grade", example = "Classified")
        String rarity,

        @Schema(description = "Skin class designation", example = "Classified Rifle")
        String skinClass,

        @Schema(description = "Selected wear condition", example = "FIELD_TESTED")
        WearCondition wearCondition,

        @Schema(description = "Selected wear price in USD", example = "18.50")
        BigDecimal priceUsd,

        @Schema(description = "Primary skin color theme", example = "RED")
        SkinColor primaryColor,

        @Schema(description = "Skin image preview URL")
        String imageUrl,

        @Schema(description = "CS:GO/CS2 Collection name", example = "The Phoenix Collection")
        String collectionName,

        @Schema(description = "Whether StatTrak variant is available for this skin", example = "true")
        Boolean statTrakAvailable,

        @Schema(description = "Paint/finish style", example = "Custom Paint Job")
        String finishStyle
) implements Serializable {

    /**
     * Backward-compatible 11-argument constructor.
     */
    public GeneratedLoadoutItemDto(
            WeaponSlot slot,
            String weaponId,
            String weaponName,
            String skinId,
            String skinName,
            String rarity,
            String skinClass,
            WearCondition wearCondition,
            BigDecimal priceUsd,
            SkinColor primaryColor,
            String imageUrl
    ) {
        this(
                slot,
                weaponId,
                weaponName,
                skinId,
                skinName,
                rarity,
                skinClass,
                wearCondition,
                priceUsd,
                primaryColor,
                imageUrl,
                "The CS2 Collection",
                true,
                "Custom Paint Job"
        );
    }

    /**
     * Backward-compatible 9-argument constructor.
     */
    public GeneratedLoadoutItemDto(
            WeaponSlot slot,
            String weaponId,
            String weaponName,
            String skinId,
            String skinName,
            WearCondition wearCondition,
            BigDecimal priceUsd,
            SkinColor primaryColor,
            String imageUrl
    ) {
        this(
                slot,
                weaponId,
                weaponName,
                skinId,
                skinName,
                null,
                null,
                wearCondition,
                priceUsd,
                primaryColor,
                imageUrl,
                "The CS2 Collection",
                true,
                "Custom Paint Job"
        );
    }
}
