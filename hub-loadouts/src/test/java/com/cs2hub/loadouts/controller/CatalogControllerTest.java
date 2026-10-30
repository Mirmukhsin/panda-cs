package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SkinDto;
import com.cs2hub.loadouts.dto.StickerDto;
import com.cs2hub.loadouts.service.CatalogService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    private CatalogController catalogController;

    @BeforeEach
    void setUp() {
        catalogController = new CatalogController(catalogService);
    }

    @Test
    @DisplayName("GET /api/v1/loadouts/catalog/skins should return 200 with paginated skins")
    void shouldSearchSkinsSuccessfully() {
        SkinDto skin = new SkinDto(
                "skin-ak47-asiimov", "AK-47 | Asiimov", "weapon_ak47", "AK-47",
                WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.ORANGE, SkinColor.WHITE,
                "https://example.com/asiimov.png", 0.05, 0.70,
                new BigDecimal("150.00"), new BigDecimal("85.00"), new BigDecimal("45.00"),
                new BigDecimal("35.00"), new BigDecimal("28.00"), new BigDecimal("28.00")
        );
        Page<SkinDto> pagedResult = new PageImpl<>(List.of(skin), PageRequest.of(0, 20), 1);

        when(catalogService.searchSkins(eq(SkinColor.ORANGE), eq("weapon_ak47"), eq(WeaponCategory.RIFLES),
                eq(TeamSide.T), any(), any(), any(Pageable.class)))
                .thenReturn(pagedResult);

        ResponseEntity<ApiResponseDto<Page<SkinDto>>> response = catalogController.searchSkins(
                SkinColor.ORANGE, "weapon_ak47", WeaponCategory.RIFLES, TeamSide.T, null, null, 0, 20, "lowestPriceUsd,asc"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().getContent()).hasSize(1);
        assertThat(response.getBody().data().getContent().get(0).name()).isEqualTo("AK-47 | Asiimov");
    }

    @Test
    @DisplayName("GET /api/v1/loadouts/catalog/skins/{skinId} should return 200 when skin exists")
    void shouldGetSkinByIdSuccessfully() {
        SkinDto skin = new SkinDto(
                "skin-m4a1s-hyper-beast", "M4A1-S | Hyper Beast", "weapon_m4a1_silencer", "M4A1-S",
                WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.PURPLE, SkinColor.BLUE,
                "https://example.com/hb.png", 0.00, 1.00,
                new BigDecimal("140.00"), new BigDecimal("75.00"), new BigDecimal("35.00"),
                new BigDecimal("25.00"), new BigDecimal("18.00"), new BigDecimal("18.00")
        );

        when(catalogService.getSkinById("skin-m4a1s-hyper-beast")).thenReturn(skin);

        ResponseEntity<ApiResponseDto<SkinDto>> response = catalogController.getSkinById("skin-m4a1s-hyper-beast");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().name()).isEqualTo("M4A1-S | Hyper Beast");
    }

    @Test
    @DisplayName("GET /api/v1/loadouts/catalog/skins/{skinId} should throw ResourceNotFoundException when missing")
    void shouldThrowNotFoundForMissingSkin() {
        when(catalogService.getSkinById("unknown")).thenThrow(new ResourceNotFoundException("Skin not found with ID: unknown"));

        assertThatThrownBy(() -> catalogController.getSkinById("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skin not found with ID: unknown");
    }

    @Test
    @DisplayName("GET /api/v1/loadouts/catalog/stickers should return 200 with paginated stickers")
    void shouldSearchStickersSuccessfully() {
        StickerDto sticker = new StickerDto(
                "sticker-howling-dawn", "Sticker | Howling Dawn", "Contraband",
                SkinColor.RED, "https://example.com/howl.png", new BigDecimal("1200.00")
        );
        Page<StickerDto> pagedResult = new PageImpl<>(List.of(sticker), PageRequest.of(0, 20), 1);

        when(catalogService.searchStickers(eq(SkinColor.RED), any(), any(), any(Pageable.class)))
                .thenReturn(pagedResult);

        ResponseEntity<ApiResponseDto<Page<StickerDto>>> response = catalogController.searchStickers(
                SkinColor.RED, null, null, 0, 20, "priceUsd,asc"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
        assertThat(response.getBody().data().getContent().get(0).name()).isEqualTo("Sticker | Howling Dawn");
    }
}
