package com.cs2hub.loadouts.service;

import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.StickerEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SkinDto;
import com.cs2hub.loadouts.dto.StickerDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.StickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SkinRepository skinRepository;

    @Mock
    private StickerRepository stickerRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(skinRepository, stickerRepository);
    }

    @Test
    @DisplayName("searchSkins should query repository with specifications and map to DTOs")
    void shouldSearchSkinsSuccessfully() {
        SkinEntity skin = SkinEntity.builder()
                .id("skin-ak47-redline")
                .name("AK-47 | Redline")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .category(WeaponCategory.RIFLES)
                .teamSide(TeamSide.T)
                .rarity("Classified")
                .primaryColor(SkinColor.RED)
                .imageUrl("https://example.com/redline.png")
                .priceFt(new BigDecimal("18.50"))
                .build();
        skin.recalculateLowestPrice();

        Pageable pageable = PageRequest.of(0, 10);
        Page<SkinEntity> entityPage = new PageImpl<>(List.of(skin), pageable, 1);

        when(skinRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);

        Page<SkinDto> result = catalogService.searchSkins(SkinColor.RED, "weapon_ak47", WeaponCategory.RIFLES, TeamSide.T, new BigDecimal("50.00"), "redline", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("skin-ak47-redline");
        assertThat(result.getContent().get(0).primaryColor()).isEqualTo(SkinColor.RED);
    }

    @Test
    @DisplayName("getSkinById should return SkinDto when found")
    void shouldReturnSkinWhenFound() {
        SkinEntity skin = SkinEntity.builder()
                .id("skin-ak47-asiimov")
                .name("AK-47 | Asiimov")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .category(WeaponCategory.RIFLES)
                .teamSide(TeamSide.T)
                .rarity("Covert")
                .primaryColor(SkinColor.ORANGE)
                .imageUrl("https://example.com/asiimov.png")
                .priceFt(new BigDecimal("45.00"))
                .build();
        skin.recalculateLowestPrice();

        when(skinRepository.findById("skin-ak47-asiimov")).thenReturn(Optional.of(skin));

        SkinDto dto = catalogService.getSkinById("skin-ak47-asiimov");

        assertThat(dto.id()).isEqualTo("skin-ak47-asiimov");
        assertThat(dto.name()).isEqualTo("AK-47 | Asiimov");
    }

    @Test
    @DisplayName("getSkinById should throw ResourceNotFoundException when skin does not exist")
    void shouldThrowNotFoundWhenSkinMissing() {
        when(skinRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getSkinById("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skin not found");
    }

    @Test
    @DisplayName("searchStickers should query repository and map to StickerDto")
    void shouldSearchStickersSuccessfully() {
        StickerEntity sticker = StickerEntity.builder()
                .id("sticker-titan-holo")
                .name("Sticker | Titan (Holo) | Katowice 2014")
                .rarity("Exotic")
                .primaryColor(SkinColor.BLUE)
                .imageUrl("https://example.com/titan.png")
                .priceUsd(new BigDecimal("45000.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<StickerEntity> entityPage = new PageImpl<>(List.of(sticker), pageable, 1);

        when(stickerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);

        Page<StickerDto> result = catalogService.searchStickers(SkinColor.BLUE, null, "titan", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("sticker-titan-holo");
        assertThat(result.getContent().get(0).primaryColor()).isEqualTo(SkinColor.BLUE);
    }

    @Test
    @DisplayName("getStickerById should throw ResourceNotFoundException when sticker missing")
    void shouldThrowNotFoundWhenStickerMissing() {
        when(stickerRepository.findById("missing-sticker")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getStickerById("missing-sticker"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sticker not found");
    }
}
