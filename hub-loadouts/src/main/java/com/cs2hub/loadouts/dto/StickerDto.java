package com.cs2hub.loadouts.dto;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.StickerEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Sticker catalog item")
public record StickerDto(
        @Schema(description = "Sticker unique identifier", example = "sticker-titan-holo-katowice-2014")
        String id,

        @Schema(description = "Sticker display name", example = "Sticker | Titan (Holo) | Katowice 2014")
        String name,

        @Schema(description = "Rarity grade", example = "Exotic")
        String rarity,

        @Schema(description = "Primary theme color", example = "BLUE")
        SkinColor primaryColor,

        @Schema(description = "Sticker image preview URL", example = "https://community.cloudflare.steamstatic.com/economy/image/...")
        String imageUrl,

        @Schema(description = "Market price in USD", example = "45000.00")
        BigDecimal priceUsd
) implements Serializable {

    public static StickerDto fromEntity(StickerEntity entity) {
        return new StickerDto(
                entity.getId(),
                entity.getName(),
                entity.getRarity(),
                entity.getPrimaryColor(),
                entity.getImageUrl(),
                entity.getPriceUsd()
        );
    }
}
