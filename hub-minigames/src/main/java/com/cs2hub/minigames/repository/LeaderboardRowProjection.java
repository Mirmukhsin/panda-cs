package com.cs2hub.minigames.repository;

import java.util.UUID;

public interface LeaderboardRowProjection {
    UUID getUserId();
    Long getTotalScore();
    Long getRoundsCount();
}
