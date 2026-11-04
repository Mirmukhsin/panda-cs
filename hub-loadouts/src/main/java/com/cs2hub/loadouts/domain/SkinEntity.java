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

    public String getSkinClass() {
        String catName = switch (category != null ? category : WeaponCategory.RIFLES) {
            case RIFLES -> "Rifle";
            case PISTOLS -> "Pistol";
            case SMGS -> "SMG";
            case HEAVY -> "Heavy";
            case KNIVES -> "Knife";
            case GLOVES -> "Glove";
        };
        if (rarity == null || rarity.isBlank()) {
            return catName;
        }
        return rarity.trim() + " " + catName;
    }

    public BigDecimal getMinPrice() {
        return lowestPriceUsd != null && lowestPriceUsd.compareTo(BigDecimal.ZERO) > 0
                ? lowestPriceUsd
                : Stream.of(priceFn, priceMw, priceFt, priceWw, priceBs)
                .filter(Objects::nonNull)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMaxPrice() {
        return Stream.of(priceFn, priceMw, priceFt, priceWw, priceBs)
                .filter(Objects::nonNull)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(getMinPrice());
    }

    public String getCollectionName() {
        if (name == null) return "The CS2 Collection";
        String n = name.toLowerCase();
        if (n.contains("fade") || n.contains("doppler") || n.contains("tiger tooth") || n.contains("marble fade") || category == WeaponCategory.KNIVES) {
            return "The Chroma Collection";
        }
        if (category == WeaponCategory.GLOVES) {
            return "The Clutch Case Collection";
        }
        if (n.contains("asiimov")) return "The Phoenix Collection";
        if (n.contains("dragon lore")) return "The Cobblestone Collection";
        if (n.contains("fire serpent")) return "The Bravo Collection";
        if (n.contains("vulcan")) return "The Huntsman Collection";
        if (n.contains("bloodsport")) return "The Spectrum Collection";
        if (n.contains("printstream")) return "The Fracture Collection";
        if (n.contains("kill confirmed")) return "The Shadow Collection";
        if (n.contains("the emperor")) return "The Prisma Collection";
        if (n.contains("howl")) return "The Huntsman Collection";
        if (n.contains("hydroponic")) return "The Rising Sun Collection";
        if (n.contains("water elemental")) return "The Breakout Collection";
        if (n.contains("neon rider")) return "The Horizon Collection";
        if (n.contains("code red")) return "The Horizon Collection";
        if (n.contains("hyper beast")) return "The Falchion Collection";
        if (n.contains("slate")) return "The Snakebite Collection";
        if (n.contains("atheris")) return "The Prisma Collection";
        if (n.contains("redline")) return "The Phoenix Collection";
        if (n.contains("cortex")) return "The Clutch Collection";
        return "The CS2 Community Collection";
    }

    public boolean isStatTrakAvailable() {
        if (category == WeaponCategory.GLOVES) return false;
        if (name != null && (name.contains("Dragon Lore") || name.contains("Hydroponic"))) return false;
        return true;
    }

    public Boolean getStatTrakAvailable() {
        return isStatTrakAvailable();
    }

    public boolean isSouvenirAvailable() {
        if (category == WeaponCategory.KNIVES || category == WeaponCategory.GLOVES) return false;
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.contains("dragon lore") || n.contains("acheron") || n.contains("mud-spec")
                || n.contains("high beam") || n.contains("candy apple") || n.contains("night terror");
    }

    public Boolean getSouvenirAvailable() {
        return isSouvenirAvailable();
    }

    public String getFinishStyle() {
        if (category == WeaponCategory.GLOVES) return "Custom Finish";
        if (name == null) return "Custom Paint Job";
        String n = name.toLowerCase();
        if (n.contains("printstream") || n.contains("slate") || n.contains("asiimov") || n.contains("emperor") || n.contains("code red")) {
            return "Custom Paint Job";
        }
        if (n.contains("doppler") || n.contains("fade") || n.contains("marble fade") || n.contains("blue phosphor")) {
            return "Anodized Multicolored";
        }
        if (n.contains("tiger tooth") || n.contains("bronze deco") || n.contains("corinthian")) {
            return "Patina";
        }
        if (n.contains("bloodsport") || n.contains("vulcan") || n.contains("neon rider") || n.contains("water elemental")) {
            return "Gunsmith";
        }
        if (n.contains("mud-spec") || n.contains("safari") || n.contains("sand dune")) {
            return "Spray-Paint";
        }
        return "Custom Paint Job";
    }
}
