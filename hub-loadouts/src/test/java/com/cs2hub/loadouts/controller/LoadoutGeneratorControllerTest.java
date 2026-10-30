package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.WeaponSlot;
import com.cs2hub.loadouts.domain.WearCondition;
import com.cs2hub.loadouts.dto.GenerateLoadoutRequestDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutItemDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutResponseDto;
import com.cs2hub.loadouts.service.BudgetLoadoutGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadoutGeneratorControllerTest {

    @Mock
    private BudgetLoadoutGeneratorService generatorService;

    private LoadoutGeneratorController controller;

    @BeforeEach
    void setUp() {
        controller = new LoadoutGeneratorController(generatorService);
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/generate should return 200 OK with generated loadout")
    void shouldGenerateBudgetLoadoutSuccessfully() {
        GenerateLoadoutRequestDto request = new GenerateLoadoutRequestDto(
                new BigDecimal("50.00"),
                SkinColor.RED,
                "weapon_m4a1_silencer",
                false
        );

        GeneratedLoadoutItemDto item = new GeneratedLoadoutItemDto(
                WeaponSlot.T_RIFLE, "weapon_ak47", "AK-47",
                "skin-ak-redline", "AK-47 | Redline", WearCondition.FT,
                new BigDecimal("18.50"), SkinColor.RED, "https://example.com/ak.png"
        );

        GeneratedLoadoutResponseDto expected = new GeneratedLoadoutResponseDto(
                SkinColor.RED,
                new BigDecimal("50.00"),
                new BigDecimal("48.00"),
                new BigDecimal("2.00"),
                List.of(item)
        );

        when(generatorService.generateLoadout(request)).thenReturn(expected);

        ResponseEntity<ApiResponseDto<GeneratedLoadoutResponseDto>> response = controller.generateLoadout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().totalCostUsd()).isEqualByComparingTo("48.00");
        assertThat(response.getBody().data().items()).hasSize(1);
    }
}
