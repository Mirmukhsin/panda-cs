package com.cs2hub.loadouts.domain;

public enum SkinColor {
    RED,
    BLUE,
    PURPLE,
    GREEN,
    YELLOW,
    ORANGE,
    WHITE,
    BLACK,
    PINK,
    GOLD;

    public static SkinColor fromString(String val) {
        if (val == null) {
            return BLACK;
        }
        try {
            return SkinColor.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BLACK;
        }
    }
}
