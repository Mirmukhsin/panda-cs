package com.cs2hub.loadouts.service;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SeedSummaryDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.StickerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private SkinRepository skinRepository;

    @Mock
    private StickerRepository stickerRepository;

    private ObjectMapper objectMapper;
    private CatalogSyncService catalogSyncService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        catalogSyncService = new CatalogSyncService(
                skinRepository,
                stickerRepository,
                null,
                objectMapper,
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/stickers.json"
        );
    }

    @Test
    @DisplayName("seedCuratedCatalog should persist >= 25 curated skins and >= 10 stickers")
    void shouldSeedCuratedCatalogSuccessfully() {
        int expectedSkins = catalogSyncService.buildCuratedSkins().size();
        int expectedStickers = catalogSyncService.buildCuratedStickers().size();
        when(skinRepository.count()).thenReturn((long) expectedSkins);
        when(stickerRepository.count()).thenReturn((long) expectedStickers);

        SeedSummaryDto result = catalogSyncService.seedCuratedCatalog();

        ArgumentCaptor<List<SkinEntity>> skinsCaptor = ArgumentCaptor.forClass(List.class);
        verify(skinRepository).saveAll(skinsCaptor.capture());
        assertThat(skinsCaptor.getValue()).hasSize(expectedSkins);
        assertThat(skinsCaptor.getValue()).hasSizeGreaterThanOrEqualTo(25);

        // Verify representative skins exist
        List<SkinEntity> savedSkins = skinsCaptor.getValue();
        assertThat(savedSkins).anyMatch(s -> s.getId().equals("skin-ak47-asiimov"));
        assertThat(savedSkins).anyMatch(s -> s.getId().equals("skin-awp-dragon-lore"));
        assertThat(savedSkins).anyMatch(s -> s.getId().equals("skin-knife-karambit-doppler"));

        verify(stickerRepository).saveAll(anyList());
        assertThat(result.skinsCount()).isEqualTo(expectedSkins);
        assertThat(result.stickersCount()).isEqualTo(expectedStickers);
        assertThat(result.message()).contains("Successfully seeded");
    }

    @Test
    @DisplayName("onStartup should trigger seedCuratedCatalog only when skin count is zero")
    void shouldSeedOnStartupWhenDatabaseIsEmpty() {
        when(skinRepository.count()).thenReturn(0L).thenReturn(29L);
        when(stickerRepository.count()).thenReturn(10L);

        catalogSyncService.onStartup();

        verify(skinRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("onStartup should not trigger seed if database already has skins and missing items exist")
    void shouldNotSeedOnStartupWhenDatabaseNotEmpty() {
        when(skinRepository.count()).thenReturn(50L);
        when(skinRepository.existsById(anyString())).thenReturn(true);
        when(stickerRepository.existsById(anyString())).thenReturn(true);

        catalogSyncService.onStartup();

        verify(skinRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CatalogSyncService should respect syncImagesOnStartup configuration")
    void shouldRespectSyncImagesOnStartup() {
        assertThat(catalogSyncService.isSyncImagesOnStartup()).isFalse();

        CatalogSyncService enabledSyncService = new CatalogSyncService(
                skinRepository,
                stickerRepository,
                null,
                objectMapper,
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/stickers.json",
                true
        );
        assertThat(enabledSyncService.isSyncImagesOnStartup()).isTrue();
    }

    @Test
    @DisplayName("SkinEntity recalculateLowestPrice should correctly detect the minimum price among wear grades")
    void shouldCalculateLowestPriceCorrectly() {
        SkinEntity skin = SkinEntity.builder()
                .id("skin-test-price")
                .name("AK-47 | Test")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .category(WeaponCategory.RIFLES)
                .teamSide(TeamSide.T)
                .rarity("Covert")
                .primaryColor(SkinColor.RED)
                .imageUrl("https://example.com/test.png")
                .priceFn(new BigDecimal("120.00"))
                .priceMw(new BigDecimal("85.50"))
                .priceFt(new BigDecimal("42.00"))
                .priceWw(new BigDecimal("35.00"))
                .priceBs(new BigDecimal("28.75"))
                .build();

        skin.recalculateLowestPrice();
        assertThat(skin.getLowestPriceUsd()).isEqualByComparingTo("28.75");

        // When lower wear grades are null
        SkinEntity skinFnOnly = SkinEntity.builder()
                .id("skin-test-fn")
                .name("AWP | FN Only")
                .weaponId("weapon_awp")
                .weaponName("AWP")
                .category(WeaponCategory.RIFLES)
                .teamSide(TeamSide.BOTH)
                .rarity("Covert")
                .primaryColor(SkinColor.PURPLE)
                .imageUrl("https://example.com/fn.png")
                .priceFn(new BigDecimal("500.00"))
                .build();

        skinFnOnly.recalculateLowestPrice();
        assertThat(skinFnOnly.getLowestPriceUsd()).isEqualByComparingTo("500.00");
    }
}
