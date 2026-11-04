package com.cs2hub.loadouts.domain;

import com.cs2hub.loadouts.dto.GeneratedLoadoutItemDto;
import com.cs2hub.loadouts.dto.SkinDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SkinMetadataTest {

    @Test
    @DisplayName("SkinEntity should correctly compute csgoskins.gg metadata helpers")
    void shouldComputeSkinMetadataHelpers() {
        SkinEntity ak = SkinEntity.builder()
                .id("skin-ak47-bloodsport")
                .name("AK-47 | Bloodsport")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .category(WeaponCategory.RIFLES)
                .teamSide(TeamSide.T)
                .rarity("Covert")
                .primaryColor(SkinColor.RED)
                .imageUrl("https://example.com/bloodsport.png")
                .minFloat(0.00)
                .maxFloat(0.45)
                .priceFn(BigDecimal.valueOf(145.00))
                .priceMw(BigDecimal.valueOf(115.00))
                .priceFt(BigDecimal.valueOf(95.00))
                .lowestPriceUsd(BigDecimal.valueOf(95.00))
                .build();

        assertThat(ak.getMinPrice()).isEqualByComparingTo(BigDecimal.valueOf(95.00));
        assertThat(ak.getMaxPrice()).isEqualByComparingTo(BigDecimal.valueOf(145.00));
        assertThat(ak.getCollectionName()).isEqualTo("The Spectrum Collection");
        assertThat(ak.isStatTrakAvailable()).isTrue();
        assertThat(ak.getStatTrakAvailable()).isTrue();
        assertThat(ak.isSouvenirAvailable()).isFalse();
        assertThat(ak.getSouvenirAvailable()).isFalse();
        assertThat(ak.getFinishStyle()).isEqualTo("Gunsmith");

        SkinDto dto = SkinDto.fromEntity(ak);
        assertThat(dto.collectionName()).isEqualTo("The Spectrum Collection");
        assertThat(dto.minPrice()).isEqualByComparingTo(BigDecimal.valueOf(95.00));
        assertThat(dto.maxPrice()).isEqualByComparingTo(BigDecimal.valueOf(145.00));
        assertThat(dto.statTrakAvailable()).isTrue();
        assertThat(dto.souvenirAvailable()).isFalse();
        assertThat(dto.finishStyle()).isEqualTo("Gunsmith");
        assertThat(dto.minFloat()).isEqualTo(0.00);
        assertThat(dto.maxFloat()).isEqualTo(0.45);
    }

    @Test
    @DisplayName("Gloves should not have StatTrak or Souvenir")
    void shouldHandleGlovesMetadata() {
        SkinEntity gloves = SkinEntity.builder()
                .id("skin-gloves-sport-vice")
                .name("Sport Gloves | Vice")
                .weaponId("weapon_gloves_sport")
                .weaponName("Sport Gloves")
                .category(WeaponCategory.GLOVES)
                .teamSide(TeamSide.BOTH)
                .rarity("Extraordinary")
                .primaryColor(SkinColor.PINK)
                .imageUrl("https://example.com/vice.png")
                .minFloat(0.06)
                .maxFloat(0.80)
                .priceFn(BigDecimal.valueOf(18000.00))
                .priceFt(BigDecimal.valueOf(2200.00))
                .lowestPriceUsd(BigDecimal.valueOf(2200.00))
                .build();

        assertThat(gloves.isStatTrakAvailable()).isFalse();
        assertThat(gloves.isSouvenirAvailable()).isFalse();
        assertThat(gloves.getCollectionName()).isEqualTo("The Clutch Case Collection");
        assertThat(gloves.getFinishStyle()).isEqualTo("Custom Finish");
    }

    @Test
    @DisplayName("GeneratedLoadoutItemDto backward compatibility constructors")
    void shouldSupportGeneratedLoadoutItemDtoConstructors() {
        GeneratedLoadoutItemDto item9 = new GeneratedLoadoutItemDto(
                WeaponSlot.T_RIFLE, "weapon_ak47", "AK-47",
                "skin-ak47-redline", "AK-47 | Redline", WearCondition.FT,
                BigDecimal.valueOf(18.50), SkinColor.RED, "https://example.com/redline.png"
        );

        assertThat(item9.collectionName()).isEqualTo("The CS2 Collection");
        assertThat(item9.statTrakAvailable()).isTrue();
        assertThat(item9.finishStyle()).isEqualTo("Custom Paint Job");

        GeneratedLoadoutItemDto item11 = new GeneratedLoadoutItemDto(
                WeaponSlot.T_RIFLE, "weapon_ak47", "AK-47",
                "skin-ak47-redline", "AK-47 | Redline", "Classified", "Classified Rifle",
                WearCondition.FT, BigDecimal.valueOf(18.50), SkinColor.RED, "https://example.com/redline.png"
        );

        assertThat(item11.rarity()).isEqualTo("Classified");
        assertThat(item11.skinClass()).isEqualTo("Classified Rifle");
        assertThat(item11.collectionName()).isEqualTo("The CS2 Collection");
        assertThat(item11.statTrakAvailable()).isTrue();
    }
}
