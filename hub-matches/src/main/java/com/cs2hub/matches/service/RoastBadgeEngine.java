package com.cs2hub.matches.service;

import com.cs2hub.matches.domain.BadgeCategory;
import com.cs2hub.matches.dto.AwardBadgeDto;
import com.cs2hub.matches.dto.MatchPlayerStatsDto;
import com.cs2hub.matches.dto.PlayerEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RoastBadgeEngine {

    public PlayerEvaluationResult evaluatePlayer(MatchPlayerStatsDto stats, int totalRounds) {
        int rounds = Math.max(totalRounds, 1);
        List<AwardBadgeDto> badges = new ArrayList<>();

        // 1. FLASHBANG_PHILANTHROPIST (ROAST)
        if (stats.teamFlashes() >= 8) {
            badges.add(new AwardBadgeDto(
                    "FLASHBANG_PHILANTHROPIST",
                    "Flashbang Philanthropist",
                    BadgeCategory.ROAST,
                    "🕶️",
                    "Blinded teammates " + stats.teamFlashes() + " times. Enemy team owes you a sponsorship."
            ));
        }

        // 2. THE_PACIFIST (ROAST)
        if (stats.zeroDamageRounds() >= 5) {
            badges.add(new AwardBadgeDto(
                    "THE_PACIFIST",
                    "The Pacifist",
                    BadgeCategory.ROAST,
                    "🕊️",
                    "Dealt 0.0 damage in " + stats.zeroDamageRounds() + " separate rounds. Just out here enjoying the scenery."
            ));
        }

        // 3. WALKING_DECOY (ROAST)
        if (stats.firstDeaths() >= 5 && stats.firstKills() == 0) {
            badges.add(new AwardBadgeDto(
                    "WALKING_DECOY",
                    "Walking Decoy",
                    BadgeCategory.ROAST,
                    "🎯",
                    "Died first in " + stats.firstDeaths() + " rounds with 0 opening kills. Human radar check."
            ));
        }

        // 4. CARDIO_ENTHUSIAST (ROAST)
        if (stats.adr() < 55.0 && stats.deaths() >= 14) {
            badges.add(new AwardBadgeDto(
                    "CARDIO_ENTHUSIAST",
                    "Cardio Enthusiast",
                    BadgeCategory.ROAST,
                    "🏃",
                    String.format(Locale.US, "%.1f ADR across %d rounds. Mostly just getting steps in.", stats.adr(), rounds)
            ));
        }

        // 5. ECO_COBRA (FLEX)
        if (stats.ecoKills() >= 5) {
            badges.add(new AwardBadgeDto(
                    "ECO_COBRA",
                    "Eco Cobra",
                    BadgeCategory.FLEX,
                    "🐍",
                    "Farmed " + stats.ecoKills() + " eco/pistol kills. Certified anti-eco merchant."
            ));
        }

        // 6. CLUTCH_MINISTER (FLEX)
        if (stats.clutchesWon() >= 2) {
            badges.add(new AwardBadgeDto(
                    "CLUTCH_MINISTER",
                    "Clutch Minister",
                    BadgeCategory.FLEX,
                    "🧊",
                    "Ice in the veins: won " + stats.clutchesWon() + " 1vX clutches when the team needed a miracle."
            ));
        }

        // 7. HEADSHOT_DEMON (FLEX)
        if (stats.kills() >= 15 && stats.headshotPct() >= 65.0) {
            badges.add(new AwardBadgeDto(
                    "HEADSHOT_DEMON",
                    "Headshot Demon",
                    BadgeCategory.FLEX,
                    "💀",
                    String.format(Locale.US, "%.1f%% headshot accuracy across %d kills. Check this player's PC.", stats.headshotPct(), stats.kills())
            ));
        }

        // 8. OPENING_BULLDOZER (FLEX)
        if (stats.firstKills() >= 5) {
            badges.add(new AwardBadgeDto(
                    "OPENING_BULLDOZER",
                    "Opening Bulldozer",
                    BadgeCategory.FLEX,
                    "🚜",
                    "Secured " + stats.firstKills() + " opening picks to crack the site wide open."
            ));
        }

        // 9. NADESHOT_CHEF (FLEX)
        if (stats.utilityDamage() >= 250) {
            badges.add(new AwardBadgeDto(
                    "NADESHOT_CHEF",
                    "Nadeshot Chef",
                    BadgeCategory.FLEX,
                    "💣",
                    "Cooked enemies for " + stats.utilityDamage() + " utility damage with HE and Molotovs."
            ));
        }

        // 10. AWP_CRUTCH (NEUTRAL)
        if (stats.kills() >= 12 && ((stats.awpKills() * 100.0) / stats.kills()) >= 60.0) {
            badges.add(new AwardBadgeDto(
                    "AWP_CRUTCH",
                    "AWP Crutch",
                    BadgeCategory.NEUTRAL,
                    "🔭",
                    stats.awpKills() + " of " + stats.kills() + " kills with the Big Green Gun. Rifling is optional."
            ));
        }

        // Fallback badge if no specific threshold was hit
        if (badges.isEmpty()) {
            if (stats.impactRating() >= 1.0) {
                AwardBadgeDto solid = new AwardBadgeDto(
                        "SOLID_SOLDIER",
                        "Solid Soldier",
                        BadgeCategory.FLEX,
                        "🎖️",
                        String.format(Locale.US, "Reliable contributor with %.2f impact rating across %d rounds.", stats.impactRating(), rounds)
                );
                badges.add(solid);
                return new PlayerEvaluationResult(
                        "Solid Soldier",
                        "Clocked in, held angles, clocked out. Honest day's Counter-Strike.",
                        badges
                );
            } else {
                AwardBadgeDto tourist = new AwardBadgeDto(
                        "LOBBY_TOURIST",
                        "Lobby Tourist",
                        BadgeCategory.ROAST,
                        "🧳",
                        String.format(Locale.US, "%d kills with %.2f impact rating across %d rounds. Nice trip to the bomb site.", stats.kills(), stats.impactRating(), rounds)
                );
                badges.add(tourist);
                return new PlayerEvaluationResult(
                        "Lobby Tourist",
                        "Took in the sights, admired the textures, occasionally fired a weapon.",
                        badges
                );
            }
        }

        // Determine headline title & roast from earned badges
        AwardBadgeDto headlineBadge = selectHeadlineBadge(badges, stats);
        return new PlayerEvaluationResult(
                headlineBadge.badgeTitle(),
                headlineBadge.description(),
                badges
        );
    }

    private AwardBadgeDto selectHeadlineBadge(List<AwardBadgeDto> badges, MatchPlayerStatsDto stats) {
        // If high impact rating (flex prioritized)
        if (stats.impactRating() >= 1.15) {
            for (AwardBadgeDto b : badges) {
                if (b.badgeCategory() == BadgeCategory.FLEX) {
                    return b;
                }
            }
        }

        // If low impact rating or roast heavy (roast prioritized)
        if (stats.impactRating() < 0.95) {
            for (AwardBadgeDto b : badges) {
                if (b.badgeCategory() == BadgeCategory.ROAST) {
                    return b;
                }
            }
        }

        // Default to first badge earned
        return badges.get(0);
    }
}
