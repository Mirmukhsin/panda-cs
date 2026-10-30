package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.WearCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Weapon item with optional sticker craft placements to save in a loadout")
public record SaveLoadoutItemDto(
        @NotBlank
        @Schema(description = "Equipment slot identifier", example = "T_RIFLE")
        String weaponSlot,

        @NotBlank
        @Schema(description = "Skin catalog identifier", example = "skin-ak47-redline")
        String skinId,

        @NotNull
        @Schema(description = "Weapon skin wear condition", example = "FIELD_TESTED")
        WearCondition wearCondition,

        @Valid
        @Size(max = 5, message = "A weapon skin can hold at most 5 stickers")
        @Schema(description = "List of up to 5 placed stickers")
        List<StickerPlacementDto> stickers
) implements Serializable {}
