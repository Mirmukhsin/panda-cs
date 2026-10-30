package com.cs2hub.minigames.service;

import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.GuessTier;
import com.cs2hub.minigames.domain.UserGuessEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EloScoringCalculatorTest {

    @Test
    @DisplayName("Diff <= 500 should award 1000 points and BULLSEYE tier (🟢)")
    void shouldScoreBullseye() {
        EloScoringCalculator.ScoreResult exact = EloScoringCalculator.calculateScore(15000, 15000);
        assertThat(exact.points()).isEqualTo(1000);
        assertThat(exact.difference()).isEqualTo(0);
        assertThat(exact.tier()).isEqualTo(GuessTier.BULLSEYE);

        EloScoringCalculator.ScoreResult boundary = EloScoringCalculator.calculateScore(15000, 15500);
        assertThat(boundary.points()).isEqualTo(1000);
        assertThat(boundary.difference()).isEqualTo(500);
        assertThat(boundary.tier()).isEqualTo(GuessTier.BULLSEYE);
    }

    @Test
    @DisplayName("Diff between 501 and 2000 should award 999 to 500 points and CLOSE tier (🟡)")
    void shouldScoreClose() {
        EloScoringCalculator.ScoreResult justAbove500 = EloScoringCalculator.calculateScore(15000, 15501);
        assertThat(justAbove500.points()).isEqualTo(999);
        assertThat(justAbove500.tier()).isEqualTo(GuessTier.CLOSE);

        EloScoringCalculator.ScoreResult middle = EloScoringCalculator.calculateScore(15000, 16250); // diff 1250: 1000 - (750*500/1500) = 750
        assertThat(middle.points()).isEqualTo(750);
        assertThat(middle.tier()).isEqualTo(GuessTier.CLOSE);

        EloScoringCalculator.ScoreResult boundary = EloScoringCalculator.calculateScore(15000, 17000); // diff 2000: 1000 - (1500*500/1500) = 500
        assertThat(boundary.points()).isEqualTo(500);
        assertThat(boundary.tier()).isEqualTo(GuessTier.CLOSE);
    }

    @Test
    @DisplayName("Diff between 2001 and 5000 should award 499 to 100 points and GRAZED tier (🟠)")
    void shouldScoreGrazed() {
        EloScoringCalculator.ScoreResult justAbove2000 = EloScoringCalculator.calculateScore(15000, 17001);
        assertThat(justAbove2000.points()).isEqualTo(499);
        assertThat(justAbove2000.tier()).isEqualTo(GuessTier.GRAZED);

        EloScoringCalculator.ScoreResult middle = EloScoringCalculator.calculateScore(15000, 18500); // diff 3500: 500 - (1500*400/3000) = 300
        assertThat(middle.points()).isEqualTo(300);
        assertThat(middle.tier()).isEqualTo(GuessTier.GRAZED);

        EloScoringCalculator.ScoreResult boundary = EloScoringCalculator.calculateScore(15000, 20000); // diff 5000: 500 - (3000*400/3000) = 100
        assertThat(boundary.points()).isEqualTo(100);
        assertThat(boundary.tier()).isEqualTo(GuessTier.GRAZED);
    }

    @Test
    @DisplayName("Diff > 5000 should award 0 points and MISS tier (🔴)")
    void shouldScoreMiss() {
        EloScoringCalculator.ScoreResult miss = EloScoringCalculator.calculateScore(15000, 20001);
        assertThat(miss.points()).isEqualTo(0);
        assertThat(miss.difference()).isEqualTo(5001);
        assertThat(miss.tier()).isEqualTo(GuessTier.MISS);

        EloScoringCalculator.ScoreResult extremeMiss = EloScoringCalculator.calculateScore(1000, 30000);
        assertThat(extremeMiss.points()).isEqualTo(0);
        assertThat(extremeMiss.tier()).isEqualTo(GuessTier.MISS);
    }

    @Test
    @DisplayName("buildShareSummary should format Discord share text with round emojis and total score")
    void shouldBuildShareSummary() {
        ClipEntity clip1 = ClipEntity.builder().actualPremierRating(15000).build();
        ClipEntity clip2 = ClipEntity.builder().actualPremierRating(16000).build();
        ClipEntity clip3 = ClipEntity.builder().actualPremierRating(10000).build();

        UserGuessEntity g1 = UserGuessEntity.builder().clip(clip1).guessedPremierRating(15200).pointsAwarded(1000).build(); // Bullseye 🟢
        UserGuessEntity g2 = UserGuessEntity.builder().clip(clip2).guessedPremierRating(17250).pointsAwarded(750).build();  // Close 🟡
        UserGuessEntity g3 = UserGuessEntity.builder().clip(clip3).guessedPremierRating(25000).pointsAwarded(0).build();    // Miss 🔴

        String summary = EloScoringCalculator.buildShareSummary(LocalDate.of(2026, 10, 30), List.of(g1, g2, g3));

        assertThat(summary).contains("🎯 CS2Hub Guess the Elo (2026-10-30)");
        assertThat(summary).contains("Round 1: 🟢 | Round 2: 🟡 | Round 3: 🔴");
        assertThat(summary).contains("Total Score: 1750 / 3000");
    }
}
