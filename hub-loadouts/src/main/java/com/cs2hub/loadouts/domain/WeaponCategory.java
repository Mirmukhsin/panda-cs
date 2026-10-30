package com.cs2hub.loadouts.domain;

public enum WeaponCategory {
    PISTOLS,
    RIFLES,
    SMGS,
    HEAVY,
    KNIVES,
    GLOVES;

    public static WeaponCategory fromString(String val) {
        if (val == null) {
            return RIFLES;
        }
        try {
            return WeaponCategory.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RIFLES;
        }
    }
}
