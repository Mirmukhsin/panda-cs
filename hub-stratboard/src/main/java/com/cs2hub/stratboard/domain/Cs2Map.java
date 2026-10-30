package com.cs2hub.stratboard.domain;

import lombok.Getter;

@Getter
public enum Cs2Map {
    DE_MIRAGE("de_mirage", "Mirage"),
    DE_INFERNO("de_inferno", "Inferno"),
    DE_DUST2("de_dust2", "Dust II"),
    DE_NUKE("de_nuke", "Nuke"),
    DE_ANCIENT("de_ancient", "Ancient"),
    DE_ANUBIS("de_anubis", "Anubis"),
    DE_TRAIN("de_train", "Train"),
    DE_VERTIGO("de_vertigo", "Vertigo");

    private final String technicalName;
    private final String displayName;

    Cs2Map(String technicalName, String displayName) {
        this.technicalName = technicalName;
        this.displayName = displayName;
    }

    public static boolean isValid(String val) {
        if (val == null) return false;
        String normalized = val.trim().toLowerCase();
        for (Cs2Map m : values()) {
            if (m.technicalName.equalsIgnoreCase(normalized) || m.name().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static Cs2Map fromString(String val) {
        if (val == null) return DE_MIRAGE;
        String normalized = val.trim().toLowerCase();
        for (Cs2Map m : values()) {
            if (m.technicalName.equalsIgnoreCase(normalized) || m.name().equalsIgnoreCase(normalized)) {
                return m;
            }
        }
        return DE_MIRAGE;
    }
}
