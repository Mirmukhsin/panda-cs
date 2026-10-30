package com.cs2hub.loadouts.domain;

public enum WearCondition {
    FN("Factory New"),
    MW("Minimal Wear"),
    FT("Field-Tested"),
    WW("Well-Worn"),
    BS("Battle-Scarred");

    private final String displayName;

    WearCondition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WearCondition fromString(String val) {
        if (val == null) {
            return FT;
        }
        try {
            return WearCondition.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FT;
        }
    }
}
