package com.cs2hub.minigames.domain;

public enum GuessTier {
    BULLSEYE("🟢"),
    CLOSE("🟡"),
    GRAZED("🟠"),
    MISS("🔴");

    private final String emoji;

    GuessTier(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}
