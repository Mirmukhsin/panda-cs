package com.cs2hub.loadouts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Curated skin market discount or trade deal")
public record DiscountDealDto(
        @Schema(description = "Unique deal ID", example = "deal-m4a4-emperor")
        String id,

        @Schema(description = "Deal title / skin name", example = "M4A4 | The Emperor (Field-Tested)")
        String title,

        @Schema(description = "Original reference price in USD", example = "25.00")
        BigDecimal originalPrice,

        @Schema(description = "Discounted sale price in USD", example = "18.75")
        BigDecimal salePrice,

        @Schema(description = "Discount percentage", example = "25")
        int discountPercent,

        @Schema(description = "Marketplace provider or store source", example = "Skinport")
        String source,

        @Schema(description = "Item preview image URL")
        String imageUrl,

        @Schema(description = "External direct deal URL", example = "https://skinport.com")
        String storeUrl
) implements Serializable {
}
