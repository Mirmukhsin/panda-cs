package com.cs2hub.loadouts.service;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.domain.WeaponSlot;
import com.cs2hub.loadouts.domain.WearCondition;
import com.cs2hub.loadouts.dto.GenerateLoadoutRequestDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutResponseDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetLoadoutGeneratorServiceTest {

    @Mock
    private SkinRepository skinRepository;

    private BudgetLoadoutGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new BudgetLoadoutGeneratorService(skinRepository);
    }

    private SkinEntity mockSkin(
            String id, String name, String weaponId, String weaponName,
            WeaponCategory category, TeamSide teamSide, SkinColor color,
            BigDecimal fn, BigDecimal mw, BigDecimal ft, BigDecimal ww, BigDecimal bs
    ) {
        SkinEntity skin = SkinEntity.builder()
                .id(id)
                .name(name)
                .weaponId(weaponId)
                .weaponName(weaponName)
                .category(category)
                .teamSide(teamSide)
                .rarity("Covert")
                .primaryColor(color)
                .imageUrl("https://example.com/" + id + ".png")
                .priceFn(fn)
                .priceMw(mw)
                .priceFt(ft)
                .priceWw(ww)
                .priceBs(bs)
                .build();
        skin.recalculateLowestPrice();
        return skin;
    }

    private void setupMockWeapons() {
        // AK-47
        SkinEntity akRedline = mockSkin("skin-ak-redline", "AK-47 | Redline", "weapon_ak47", "AK-47",
                WeaponCategory.RIFLES, TeamSide.T, SkinColor.RED,
                null, new BigDecimal("35.00"), new BigDecimal("18.50"), new BigDecimal("16.00"), new BigDecimal("14.00"));

        SkinEntity akSlate = mockSkin("skin-ak-slate", "AK-47 | Slate", "weapon_ak47", "AK-47",
                WeaponCategory.RIFLES, TeamSide.T, SkinColor.BLACK,
                new BigDecimal("14.50"), new BigDecimal("7.80"), new BigDecimal("4.20"), new BigDecimal("3.50"), new BigDecimal("2.90"));

        when(skinRepository.findAllByWeaponId("weapon_ak47")).thenReturn(List.of(akRedline, akSlate));

        // M4A1-S
        SkinEntity m4Cyrex = mockSkin("skin-m4-cyrex", "M4A1-S | Cyrex", "weapon_m4a1_silencer", "M4A1-S",
                WeaponCategory.RIFLES, TeamSide.CT, SkinColor.RED,
                new BigDecimal("55.00"), new BigDecimal("32.00"), new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("16.00"));

        when(skinRepository.findAllByWeaponId("weapon_m4a1_silencer")).thenReturn(List.of(m4Cyrex));

        // AWP
        SkinEntity awpWildfire = mockSkin("skin-awp-wildfire", "AWP | Wildfire", "weapon_awp", "AWP",
                WeaponCategory.RIFLES, TeamSide.BOTH, SkinColor.RED,
                new BigDecimal("180.00"), new BigDecimal("105.00"), new BigDecimal("60.00"), new BigDecimal("50.00"), new BigDecimal("42.00"));

        SkinEntity awpAtheris = mockSkin("skin-awp-atheris", "AWP | Atheris", "weapon_awp", "AWP",
                WeaponCategory.RIFLES, TeamSide.BOTH, SkinColor.GREEN,
                new BigDecimal("18.00"), new BigDecimal("8.00"), new BigDecimal("4.50"), new BigDecimal("3.80"), new BigDecimal("3.20"));

        when(skinRepository.findAllByWeaponId("weapon_awp")).thenReturn(List.of(awpWildfire, awpAtheris));

        // Glock
        SkinEntity glockWater = mockSkin("skin-glock-water", "Glock-18 | Water Elemental", "weapon_glock", "Glock-18",
                WeaponCategory.PISTOLS, TeamSide.T, SkinColor.RED,
                new BigDecimal("16.00"), new BigDecimal("11.00"), new BigDecimal("7.50"), new BigDecimal("7.00"), new BigDecimal("6.50"));

        when(skinRepository.findAllByWeaponId("weapon_glock")).thenReturn(List.of(glockWater));

        // USP-S
        SkinEntity uspsTicket = mockSkin("skin-usps-ticket", "USP-S | Ticket to Hell", "weapon_usp_silencer", "USP-S",
                WeaponCategory.PISTOLS, TeamSide.CT, SkinColor.WHITE,
                new BigDecimal("6.00"), new BigDecimal("2.50"), new BigDecimal("1.20"), new BigDecimal("1.00"), new BigDecimal("0.90"));

        when(skinRepository.findAllByWeaponId("weapon_usp_silencer")).thenReturn(List.of(uspsTicket));

        // Deagle
        SkinEntity deagleCodeRed = mockSkin("skin-deagle-code-red", "Desert Eagle | Code Red", "weapon_deagle", "Desert Eagle",
                WeaponCategory.PISTOLS, TeamSide.BOTH, SkinColor.RED,
                new BigDecimal("85.00"), new BigDecimal("45.00"), new BigDecimal("28.00"), new BigDecimal("24.00"), new BigDecimal("20.00"));

        when(skinRepository.findAllByWeaponId("weapon_deagle")).thenReturn(List.of(deagleCodeRed));

        // MAC-10
        SkinEntity mac10 = mockSkin("skin-mac10-disco", "MAC-10 | Disco Tech", "weapon_mac10", "MAC-10",
                WeaponCategory.SMGS, TeamSide.T, SkinColor.PURPLE,
                new BigDecimal("20.00"), new BigDecimal("8.50"), new BigDecimal("4.20"), new BigDecimal("3.50"), new BigDecimal("3.00"));

        when(skinRepository.findAllByWeaponId("weapon_mac10")).thenReturn(List.of(mac10));

        // MP9
        SkinEntity mp9 = mockSkin("skin-mp9-starlight", "MP9 | Starlight Protector", "weapon_mp9", "MP9",
                WeaponCategory.SMGS, TeamSide.CT, SkinColor.WHITE,
                new BigDecimal("35.00"), new BigDecimal("16.00"), new BigDecimal("8.50"), new BigDecimal("7.00"), new BigDecimal("6.00"));

        when(skinRepository.findAllByWeaponId("weapon_mp9")).thenReturn(List.of(mp9));
    }

    @Test
    @DisplayName("generateLoadout should strictly enforce budget ceiling (totalCostUsd <= budgetUsd)")
    void shouldStrictlyEnforceBudgetCeiling() {
        setupMockWeapons();

        GenerateLoadoutRequestDto request = new GenerateLoadoutRequestDto(
                new BigDecimal("75.00"),
                SkinColor.RED,
                "weapon_m4a1_silencer",
                false
        );

        GeneratedLoadoutResponseDto result = generatorService.generateLoadout(request);

        assertThat(result.themeColor()).isEqualTo(SkinColor.RED);
        assertThat(result.budgetLimitUsd()).isEqualByComparingTo("75.00");
        assertThat(result.totalCostUsd()).isLessThanOrEqualTo(new BigDecimal("75.00"));
        assertThat(result.remainingBudgetUsd()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.items()).isNotEmpty();

        // Total cost + remaining budget must equal budget limit
        assertThat(result.totalCostUsd().add(result.remainingBudgetUsd())).isEqualByComparingTo(result.budgetLimitUsd());
    }

    @Test
    @DisplayName("generateLoadout should prefer theme color matched skins when affordable")
    void shouldPreferThemeColorMatchedSkins() {
        setupMockWeapons();

        GenerateLoadoutRequestDto request = new GenerateLoadoutRequestDto(
                new BigDecimal("100.00"),
                SkinColor.RED,
                "weapon_m4a1_silencer",
                false
        );

        GeneratedLoadoutResponseDto result = generatorService.generateLoadout(request);

        // AK-47 slot should pick Redline over Slate because RED matches theme
        assertThat(result.items()).anyMatch(i -> i.slot() == WeaponSlot.T_RIFLE && i.skinId().equals("skin-ak-redline"));
        assertThat(result.items()).anyMatch(i -> i.slot() == WeaponSlot.CT_RIFLE && i.skinId().equals("skin-m4-cyrex"));
    }

    @Test
    @DisplayName("generateLoadout should upgrade wear conditions in optimization pass when budget remains")
    void shouldOptimizeWearConditionsWithRemainingBudget() {
        setupMockWeapons();

        GenerateLoadoutRequestDto highBudgetReq = new GenerateLoadoutRequestDto(
                new BigDecimal("300.00"),
                SkinColor.RED,
                "weapon_m4a1_silencer",
                false
        );

        GeneratedLoadoutResponseDto highResult = generatorService.generateLoadout(highBudgetReq);

        // With $300 budget, AWP and M4A1-S should be upgraded to Factory New or Minimal Wear
        assertThat(highResult.totalCostUsd()).isLessThanOrEqualTo(new BigDecimal("300.00"));
        assertThat(highResult.items()).anyMatch(i -> i.wearCondition() == WearCondition.FN || i.wearCondition() == WearCondition.MW);
    }

    @Test
    @DisplayName("generateLoadout should generate non-empty loadout for $10.00 budget with RED theme")
    void shouldGenerateNonEmptyLoadoutForTenDollarRedBudget() {
        setupMockWeapons();

        SkinEntity uspsCheckEngine = mockSkin("skin-usps-checkengine", "USP-S | Check Engine", "weapon_usp_silencer", "USP-S",
                WeaponCategory.PISTOLS, TeamSide.CT, SkinColor.RED,
                new BigDecimal("2.50"), new BigDecimal("1.80"), new BigDecimal("1.20"), new BigDecimal("1.00"), new BigDecimal("0.80"));
        SkinEntity m4NightTerror = mockSkin("skin-m4-nightterror", "M4A1-S | Night Terror", "weapon_m4a1_silencer", "M4A1-S",
                WeaponCategory.RIFLES, TeamSide.CT, SkinColor.RED,
                new BigDecimal("2.50"), new BigDecimal("1.50"), new BigDecimal("0.90"), new BigDecimal("0.70"), new BigDecimal("0.60"));

        when(skinRepository.findAllByWeaponId("weapon_usp_silencer")).thenReturn(List.of(uspsCheckEngine));
        when(skinRepository.findAllByWeaponId("weapon_m4a1_silencer")).thenReturn(List.of(m4NightTerror));

        GenerateLoadoutRequestDto request = new GenerateLoadoutRequestDto(
                new BigDecimal("10.00"),
                SkinColor.RED,
                "weapon_m4a1_silencer",
                false
        );

        GeneratedLoadoutResponseDto result = generatorService.generateLoadout(request);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.totalCostUsd()).isLessThanOrEqualTo(new BigDecimal("10.00"));
        assertThat(result.totalCostUsd()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.themeColor()).isEqualTo(SkinColor.RED);
        assertThat(result.items()).anyMatch(i -> i.skinName().contains("Night Terror") || i.skinName().contains("Water Elemental") || i.skinName().contains("Check Engine"));
    }
}
