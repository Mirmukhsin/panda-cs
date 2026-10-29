package com.cs2hub.minigames.domain;

public enum VideoProvider {
    YOUTUBE,
    TWITCH,
    MEDAL,
    DIRECT;

    public static VideoProvider fromUrl(String url) {
        if (url == null) {
            return DIRECT;
        }
        String lower = url.toLowerCase();
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return YOUTUBE;
        }
        if (lower.contains("twitch.tv") || lower.contains("clips.twitch.tv")) {
            return TWITCH;
        }
        if (lower.contains("medal.tv")) {
            return MEDAL;
        }
        return DIRECT;
    }
}
