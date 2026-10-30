package com.cs2hub.loadouts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

@Entity
@Table(name = "skins", schema = "loadouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinEntity {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "weapon_id", nullable = false, length = 64)
    private String weaponId;

    @Column(name = "weapon_name", nullable = false, length = 128)
    private String weaponName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private WeaponCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 16)
    private TeamSide teamSide;

    @Column(name = "rarity", nullable = false, length = 64)
    private String rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_color", nullable = false, length = 32)
    private SkinColor primaryColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_color", length = 32)
    private SkinColor secondaryColor;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    @Column(name = "min_float")
    private Double minFloat = 0.0;

    @Builder.Default
    @Column(name = "max_float")
    private Double maxFloat = 1.0;

    @Column(name = "price_fn", precision = 10, scale = 2)
    private BigDecimal priceFn;

    @Column(name = "price_mw", precision = 10, scale = 2)
    private BigDecimal priceMw;

    @Column(name = "price_ft", precision = 10, scale = 2)
    private BigDecimal priceFt;

    @Column(name = "price_ww", precision = 10, scale = 2)
    private BigDecimal priceWw;

    @Column(name = "price_bs", precision = 10, scale = 2)
    private BigDecimal priceBs;

    @Builder.Default
    @Column(name = "lowest_price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal lowestPriceUsd = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void computeLowestPriceAndTimestamp() {
        this.lowestPriceUsd = Stream.of(priceFn, priceMw, priceFt, priceWw, priceBs)
                .filter(Objects::nonNull)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        this.updatedAt = Instant.now();
    }

    public void recalculateLowestPrice() {
        computeLowestPriceAndTimestamp();
    }

    public BigDecimal getExactPriceForCondition(WearCondition condition) {
        if (condition == null) return null;
        BigDecimal price = switch (condition) {
            case FN -> priceFn;
            case MW -> priceMw;
            case FT -> priceFt;
            case WW -> priceWw;
            case BS -> priceBs;
        };
        return (price != null && price.compareTo(BigDecimal.ZERO) > 0) ? price : null;
    }

    public BigDecimal getPriceForCondition(WearCondition condition) {
        BigDecimal exact = getExactPriceForCondition(condition);
        return exact != null ? exact : lowestPriceUsd;
    }
}
