package com.cs2hub.minigames.service;

import com.cs2hub.minigames.domain.GuessTier;
import com.cs2hub.minigames.domain.UserGuessEntity;

import java.time.LocalDate;
import java.util.List;

public final class EloScoringCalculator {

    private EloScoringCalculator() {}

    public record ScoreResult(int points, int difference, GuessTier tier) {}

    public static ScoreResult calculateScore(int actualRating, int guessedRating) {
        int diff = Math.abs(actualRating - guessedRating);

        if (diff <= 500) {
            return new ScoreResult(1000, diff, GuessTier.BULLSEYE);
        } else if (diff <= 2000) {
            int points = 1000 - ((diff - 500) * 500 / 1500);
            points = Math.min(999, Math.max(500, points));
            return new ScoreResult(points, diff, GuessTier.CLOSE);
        } else if (diff <= 5000) {
            int points = 500 - ((diff - 2000) * 400 / 3000);
            points = Math.min(499, Math.max(100, points));
            return new ScoreResult(points, diff, GuessTier.GRAZED);
        } else {
            return new ScoreResult(0, diff, GuessTier.MISS);
        }
    }

    public static GuessTier determineTier(int actualRating, int guessedRating) {
        return calculateScore(actualRating, guessedRating).tier();
    }

    public static String buildShareSummary(LocalDate date, List<UserGuessEntity> guesses) {
        if (guesses == null || guesses.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 CS2Hub Guess the Elo (").append(date).append(")\n");

        int totalScore = 0;
        for (int i = 0; i < guesses.size(); i++) {
            UserGuessEntity guess = guesses.get(i);
            totalScore += guess.getPointsAwarded();

            GuessTier tier;
            if (guess.getClip() != null && guess.getClip().getActualPremierRating() != null) {
                tier = determineTier(guess.getClip().getActualPremierRating(), guess.getGuessedPremierRating());
            } else {
                int pts = guess.getPointsAwarded();
                if (pts >= 1000) tier = GuessTier.BULLSEYE;
                else if (pts >= 500) tier = GuessTier.CLOSE;
                else if (pts >= 100) tier = GuessTier.GRAZED;
                else tier = GuessTier.MISS;
            }

            sb.append("Round ").append(i + 1).append(": ").append(tier.getEmoji());
            if (i < guesses.size() - 1) {
                sb.append(" | ");
            }
        }

        sb.append("\nTotal Score: ").append(totalScore).append(" / ").append(guesses.size() * 1000);
        return sb.toString();
    }
}
