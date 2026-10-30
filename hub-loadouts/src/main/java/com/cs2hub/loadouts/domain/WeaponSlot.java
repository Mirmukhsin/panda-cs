package com.cs2hub.loadouts.domain;

import lombok.Getter;

@Getter
public enum WeaponSlot {
    T_RIFLE("weapon_ak47", "AK-47", WeaponCategory.RIFLES, 0.25, false),
    CT_RIFLE("weapon_m4a1_silencer", "M4A1-S / M4A4", WeaponCategory.RIFLES, 0.25, false),
    AWP("weapon_awp", "AWP", WeaponCategory.RIFLES, 0.20, false),
    T_PISTOL("weapon_glock", "Glock-18", WeaponCategory.PISTOLS, 0.08, false),
    CT_PISTOL("weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, 0.08, false),
    DEAGLE("weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, 0.08, false),
    T_SMG("weapon_mac10", "MAC-10", WeaponCategory.SMGS, 0.03, false),
    CT_SMG("weapon_mp9", "MP9", WeaponCategory.SMGS, 0.03, false),
    KNIFE(null, "Knife", WeaponCategory.KNIVES, 0.0, true),
    GLOVES(null, "Gloves", WeaponCategory.GLOVES, 0.0, true);

    private final String defaultWeaponId;
    private final String displayName;
    private final WeaponCategory category;
    private final double budgetWeight;
    private final boolean luxury;

    WeaponSlot(String defaultWeaponId, String displayName, WeaponCategory category, double budgetWeight, boolean luxury) {
        this.defaultWeaponId = defaultWeaponId;
        this.displayName = displayName;
        this.category = category;
        this.budgetWeight = budgetWeight;
        this.luxury = luxury;
    }
}
