package com.cs2hub.loadouts.domain;

public enum TeamSide {
    T,
    CT,
    BOTH;

    public static TeamSide fromString(String val) {
        if (val == null) {
            return BOTH;
        }
        try {
            return TeamSide.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOTH;
        }
    }
}
