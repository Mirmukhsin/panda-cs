package com.cs2hub.loadouts.service;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.domain.WeaponSlot;
import com.cs2hub.loadouts.domain.WearCondition;
import com.cs2hub.loadouts.dto.GenerateLoadoutRequestDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutItemDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutResponseDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetLoadoutGeneratorService {

    private final SkinRepository skinRepository;

    private static final WearCondition[] WEAR_QUALITY_ORDER = {
            WearCondition.FN,
            WearCondition.MW,
            WearCondition.FT,
            WearCondition.WW,
            WearCondition.BS
    };

    public GeneratedLoadoutResponseDto generateLoadout(GenerateLoadoutRequestDto request) {
        BigDecimal budgetLimit = request.budgetUsd().setScale(2, RoundingMode.HALF_UP);
        SkinColor themeColor = request.themeColor();
        String preferredCtRifle = StringUtils.hasText(request.preferredCtRifle())
                ? request.preferredCtRifle().trim().toLowerCase()
                : "weapon_m4a1_silencer";
        boolean includeLuxury = Boolean.TRUE.equals(request.includeKnifeAndGloves());

        List<WeaponSlot> slotsToFill = new ArrayList<>();
        slotsToFill.add(WeaponSlot.T_RIFLE);
        slotsToFill.add(WeaponSlot.CT_RIFLE);
        slotsToFill.add(WeaponSlot.AWP);
        slotsToFill.add(WeaponSlot.T_PISTOL);
        slotsToFill.add(WeaponSlot.CT_PISTOL);
        slotsToFill.add(WeaponSlot.DEAGLE);
        slotsToFill.add(WeaponSlot.T_SMG);
        slotsToFill.add(WeaponSlot.CT_SMG);

        if (includeLuxury) {
            slotsToFill.add(WeaponSlot.KNIFE);
            slotsToFill.add(WeaponSlot.GLOVES);
        }

        // Cache candidate options per slot
        Map<WeaponSlot, List<CandidateOption>> candidatesBySlot = new HashMap<>();
        for (WeaponSlot slot : slotsToFill) {
            String targetWeaponId = resolveWeaponId(slot, preferredCtRifle);
            candidatesBySlot.put(slot, findCandidatesForSlot(slot, targetWeaponId, themeColor));
        }

        List<SelectedSlotState> selectedSlots = new ArrayList<>();
        BigDecimal remainingBudget = budgetLimit;

        // Pass 1: Greedy Allocation with Reserve for Later Slots
        for (int i = 0; i < slotsToFill.size(); i++) {
            WeaponSlot slot = slotsToFill.get(i);
            List<CandidateOption> candidates = candidatesBySlot.get(slot);
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            // Calculate minimum budget needed for remaining core slots
            BigDecimal minReserveForRemaining = BigDecimal.ZERO;
            for (int j = i + 1; j < slotsToFill.size(); j++) {
                WeaponSlot remSlot = slotsToFill.get(j);
                if (!remSlot.isLuxury()) {
                    List<CandidateOption> remCandidates = candidatesBySlot.get(remSlot);
                    CandidateOption cheapestRem = getCheapestOption(remCandidates);
                    if (cheapestRem != null) {
                        minReserveForRemaining = minReserveForRemaining.add(cheapestRem.price());
                    }
                }
            }

            BigDecimal slotTargetBudget = budgetLimit.multiply(BigDecimal.valueOf(slot.getBudgetWeight()))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal maxSpendableForThisSlot = remainingBudget.subtract(minReserveForRemaining);
            if (maxSpendableForThisSlot.compareTo(BigDecimal.ZERO) <= 0) {
                // If forward reservation is too tight for low budgets (e.g. $5 - $10),
                // don't starve current slot; allow spending up to remainingBudget
                maxSpendableForThisSlot = remainingBudget;
            }

            final BigDecimal currentRemaining = remainingBudget;
            final BigDecimal targetBudget = slotTargetBudget;

            // Candidate options that fit within maxSpendableForThisSlot
            BigDecimal effectiveCap = maxSpendableForThisSlot;
            List<CandidateOption> affordableOptions = candidates.stream()
                    .filter(c -> c.price().compareTo(effectiveCap) <= 0)
                    .toList();

            // If none fit within effectiveCap, fall back to any option within remainingBudget
            if (affordableOptions.isEmpty()) {
                affordableOptions = candidates.stream()
                        .filter(c -> c.price().compareTo(currentRemaining) <= 0)
                        .toList();
            }

            if (affordableOptions.isEmpty()) {
                // If slot is luxury or budget is exhausted, skip slot
                continue;
            }

            final List<CandidateOption> finalAffordable = affordableOptions;

            // Select best candidate from affordable options:
            // 1. Try matching theme color within slotTargetBudget
            // 2. Try matching theme color within effectiveCap / remainingBudget (preferring cheaper among matches)
            // 3. Fall back to best wear / lowest price neutral skin within remainingBudget
            CandidateOption chosen = finalAffordable.stream()
                    .filter(c -> c.matchScore() > 0 && c.price().compareTo(targetBudget) <= 0)
                    .max(Comparator.comparingInt(CandidateOption::matchScore)
                            .thenComparing(CandidateOption::wearQualityScore)
                            .thenComparing(CandidateOption::price))
                    .orElseGet(() -> finalAffordable.stream()
                            .filter(c -> c.matchScore() > 0)
                            .max(Comparator.comparingInt(CandidateOption::matchScore)
                                    .thenComparing(CandidateOption::wearQualityScore)
                                    .thenComparing(c -> c.price().negate()))
                            .orElseGet(() -> finalAffordable.stream()
                                    .max(Comparator.comparing(CandidateOption::wearQualityScore)
                                            .thenComparing(c -> c.price().negate()))
                                    .orElse(finalAffordable.get(0))));

            selectedSlots.add(new SelectedSlotState(slot, chosen.skin(), chosen.wear(), chosen.price()));
            remainingBudget = remainingBudget.subtract(chosen.price());
        }

        // Downgrade Pass: Ensure totalCost <= budgetLimit under all circumstances
        while (calculateTotalCost(selectedSlots).compareTo(budgetLimit) > 0) {
            Optional<SelectedSlotState> slotWithCheaperOption = selectedSlots.stream()
                    .filter(s -> hasCheaperOption(s, candidatesBySlot.get(s.slot)))
                    .max(Comparator.comparing(s -> s.price));

            if (slotWithCheaperOption.isPresent()) {
                SelectedSlotState state = slotWithCheaperOption.get();
                CandidateOption cheaper = getNextCheaperOption(state, candidatesBySlot.get(state.slot));
                state.skin = cheaper.skin();
                state.wear = cheaper.wear();
                state.price = cheaper.price();
            } else {
                // Never drop down to 0 items! If only 1 slot remains, stop dropping
                if (selectedSlots.size() <= 1) {
                    break;
                }
                Optional<SelectedSlotState> lowestPrioritySlot = selectedSlots.stream()
                        .min(Comparator.comparingDouble(s -> s.slot.getBudgetWeight()));
                if (lowestPrioritySlot.isPresent()) {
                    selectedSlots.remove(lowestPrioritySlot.get());
                } else {
                    break;
                }
            }
        }

        // Low-budget safeguard: If selectedSlots is empty, find any skin fitting within budgetLimit
        if (selectedSlots.isEmpty()) {
            for (WeaponSlot slot : slotsToFill) {
                List<CandidateOption> candidates = candidatesBySlot.get(slot);
                if (candidates == null) continue;

                Optional<CandidateOption> budgetFit = candidates.stream()
                        .filter(c -> c.price().compareTo(budgetLimit) <= 0)
                        .max(Comparator.comparingInt(CandidateOption::matchScore)
                                .thenComparing(CandidateOption::wearQualityScore)
                                .thenComparing(c -> c.price().negate()));

                if (budgetFit.isPresent()) {
                    CandidateOption chosen = budgetFit.get();
                    selectedSlots.add(new SelectedSlotState(slot, chosen.skin(), chosen.wear(), chosen.price()));
                    remainingBudget = budgetLimit.subtract(chosen.price());
                    break;
                }
            }
        }

        // Pass 2: Budget Optimization & Wear Upgrades
        // Use any remaining budget to upgrade wear condition or skin tier
        boolean upgraded;
        do {
            upgraded = false;
            BigDecimal currentTotal = calculateTotalCost(selectedSlots);
            BigDecimal currentRemaining = budgetLimit.subtract(currentTotal);

            if (currentRemaining.compareTo(BigDecimal.valueOf(0.20)) <= 0) {
                break;
            }

            // Prioritize upgrading in order: AWP, T_RIFLE, CT_RIFLE, DEAGLE, PISTOLS, SMGS
            List<SelectedSlotState> upgradeCandidates = new ArrayList<>(selectedSlots);
            upgradeCandidates.sort(Comparator.comparingDouble((SelectedSlotState s) -> s.slot.getBudgetWeight()).reversed());

            for (SelectedSlotState state : upgradeCandidates) {
                List<CandidateOption> candidates = candidatesBySlot.get(state.slot);
                if (candidates == null) continue;

                // Find a strictly better option
                Optional<CandidateOption> bestUpgrade = candidates.stream()
                        .filter(c -> c.price().compareTo(state.price) > 0)
                        .filter(c -> c.price().subtract(state.price).compareTo(currentRemaining) <= 0)
                        .filter(c -> c.matchScore() >= state.getMatchScore(themeColor))
                        .max(Comparator.comparing(CandidateOption::wearQualityScore)
                                .thenComparing(CandidateOption::price));

                if (bestUpgrade.isPresent()) {
                    CandidateOption upgrade = bestUpgrade.get();
                    state.skin = upgrade.skin();
                    state.wear = upgrade.wear();
                    state.price = upgrade.price();
                    upgraded = true;
                    break; // re-evaluate remaining budget
                }
            }
        } while (upgraded);

        // Build Response
        BigDecimal totalCost = calculateTotalCost(selectedSlots);
        BigDecimal finalRemaining = budgetLimit.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

        List<GeneratedLoadoutItemDto> items = selectedSlots.stream()
                .map(s -> new GeneratedLoadoutItemDto(
                        s.slot,
                        s.skin.getWeaponId(),
                        s.skin.getWeaponName(),
                        s.skin.getId(),
                        s.skin.getName(),
                        s.wear,
                        s.price,
                        s.skin.getPrimaryColor(),
                        s.skin.getImageUrl()
                ))
                .toList();

        return new GeneratedLoadoutResponseDto(themeColor, budgetLimit, totalCost, finalRemaining, items);
    }

    private BigDecimal calculateTotalCost(List<SelectedSlotState> slots) {
        return slots.stream()
                .map(s -> s.price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private CandidateOption getCheapestOption(List<CandidateOption> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.stream()
                .min(Comparator.comparing(CandidateOption::price))
                .orElse(null);
    }

    private boolean hasCheaperOption(SelectedSlotState state, List<CandidateOption> candidates) {
        if (candidates == null) return false;
        return candidates.stream().anyMatch(c -> c.price().compareTo(state.price) < 0);
    }

    private CandidateOption getNextCheaperOption(SelectedSlotState state, List<CandidateOption> candidates) {
        return candidates.stream()
                .filter(c -> c.price().compareTo(state.price) < 0)
                .max(Comparator.comparing(CandidateOption::price))
                .orElseGet(() -> getCheapestOption(candidates));
    }

    private String resolveWeaponId(WeaponSlot slot, String preferredCtRifle) {
        if (slot == WeaponSlot.CT_RIFLE) {
            return preferredCtRifle;
        }
        return slot.getDefaultWeaponId();
    }

    private List<CandidateOption> findCandidatesForSlot(WeaponSlot slot, String weaponId, SkinColor themeColor) {
        List<SkinEntity> skins;
        if (slot.getCategory() == WeaponCategory.KNIVES || slot.getCategory() == WeaponCategory.GLOVES) {
            skins = skinRepository.findAllByCategory(slot.getCategory());
        } else {
            skins = skinRepository.findAllByWeaponId(weaponId);
            if (skins.isEmpty() && slot == WeaponSlot.CT_RIFLE) {
                skins = skinRepository.findAllByWeaponId("weapon_m4a1_silencer");
                if (skins.isEmpty()) {
                    skins = skinRepository.findAllByWeaponId("weapon_m4a4");
                }
            }
        }

        List<CandidateOption> options = new ArrayList<>();
        for (SkinEntity skin : skins) {
            int matchScore = 0;
            if (skin.getPrimaryColor() == themeColor) {
                matchScore = 2;
            } else if (skin.getSecondaryColor() == themeColor) {
                matchScore = 1;
            }

            for (WearCondition wear : WEAR_QUALITY_ORDER) {
                BigDecimal price = skin.getExactPriceForCondition(wear);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    options.add(new CandidateOption(skin, wear, price, matchScore));
                }
            }
        }

        // Sort candidates: highest match score first, then wear quality, then price ascending
        options.sort(Comparator.comparingInt(CandidateOption::matchScore).reversed()
                .thenComparing(CandidateOption::wearQualityScore, Comparator.reverseOrder())
                .thenComparing(CandidateOption::price));

        return options;
    }

    private record CandidateOption(SkinEntity skin, WearCondition wear, BigDecimal price, int matchScore) {
        public int wearQualityScore() {
            return switch (wear) {
                case FN -> 5;
                case MW -> 4;
                case FT -> 3;
                case WW -> 2;
                case BS -> 1;
            };
        }
    }

    private static class SelectedSlotState {
        private final WeaponSlot slot;
        private SkinEntity skin;
        private WearCondition wear;
        private BigDecimal price;

        public SelectedSlotState(WeaponSlot slot, SkinEntity skin, WearCondition wear, BigDecimal price) {
            this.slot = slot;
            this.skin = skin;
            this.wear = wear;
            this.price = price;
        }

        public int getMatchScore(SkinColor themeColor) {
            if (skin.getPrimaryColor() == themeColor) return 2;
            if (skin.getSecondaryColor() == themeColor) return 1;
            return 0;
        }
    }
}
