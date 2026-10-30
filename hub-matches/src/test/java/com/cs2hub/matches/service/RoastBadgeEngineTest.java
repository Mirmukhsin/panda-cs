package com.cs2hub.matches.service;

import com.cs2hub.matches.domain.BadgeCategory;
import com.cs2hub.matches.domain.MatchTeamSide;
import com.cs2hub.matches.dto.AwardBadgeDto;
import com.cs2hub.matches.dto.MatchPlayerStatsDto;
import com.cs2hub.matches.dto.PlayerEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoastBadgeEngineTest {

    private RoastBadgeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RoastBadgeEngine();
    }

    private MatchPlayerStatsDto baseStats() {
        return new MatchPlayerStatsDto(
                "76561198000000001", "Player", MatchTeamSide.TEAM_A,
                10, 10, 2, 65.0, 40.0, 60.0, 1.0,
                0, 0, 0, 0, 5, 50, 0, 0, 0, 0, 0
        );
    }

    @Test
    @DisplayName("Should assign FLASHBANG_PHILANTHROPIST when teamFlashes >= 8")
    void shouldTriggerFlashbangPhilanthropist() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Flasher", MatchTeamSide.TEAM_A,
                10, 10, 2, 65.0, 40.0, 60.0, 0.85,
                0, 0, 0, 9, 5, 50, 0, 0, 0, 0, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("FLASHBANG_PHILANTHROPIST"));
        assertThat(result.headlineTitle()).isEqualTo("Flashbang Philanthropist");
        assertThat(result.headlineRoast()).contains("Blinded teammates 9 times");
    }

    @Test
    @DisplayName("Should assign THE_PACIFIST when zeroDamageRounds >= 5")
    void shouldTriggerThePacifist() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Pacifist", MatchTeamSide.TEAM_A,
                5, 12, 1, 40.0, 20.0, 40.0, 0.70,
                0, 1, 0, 0, 2, 10, 6, 0, 0, 0, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("THE_PACIFIST"));
        assertThat(result.headlineRoast()).contains("Dealt 0.0 damage in 6 separate rounds");
    }

    @Test
    @DisplayName("Should assign WALKING_DECOY when firstDeaths >= 5 and firstKills == 0")
    void shouldTriggerWalkingDecoy() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Decoy", MatchTeamSide.TEAM_A,
                8, 15, 2, 55.0, 30.0, 50.0, 0.75,
                0, 6, 0, 0, 4, 30, 2, 0, 0, 0, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("WALKING_DECOY"));
        assertThat(result.headlineRoast()).contains("Died first in 6 rounds with 0 opening kills");
    }

    @Test
    @DisplayName("Should assign CARDIO_ENTHUSIAST when adr < 55.0 and deaths >= 14")
    void shouldTriggerCardioEnthusiast() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Runner", MatchTeamSide.TEAM_A,
                7, 16, 3, 49.5, 30.0, 45.0, 0.68,
                1, 2, 0, 1, 4, 20, 2, 0, 0, 0, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 22);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("CARDIO_ENTHUSIAST"));
        assertThat(result.headlineRoast()).contains("49.5 ADR across 22 rounds");
    }

    @Test
    @DisplayName("Should assign ECO_COBRA when ecoKills >= 5")
    void shouldTriggerEcoCobra() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Farmer", MatchTeamSide.TEAM_A,
                16, 12, 4, 85.0, 45.0, 70.0, 1.20,
                2, 1, 1, 0, 10, 60, 1, 6, 0, 1, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("ECO_COBRA"));
        assertThat(result.headlineRoast()).contains("Farmed 6 eco/pistol kills");
    }

    @Test
    @DisplayName("Should assign CLUTCH_MINISTER when clutchesWon >= 2")
    void shouldTriggerClutchMinister() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Icy", MatchTeamSide.TEAM_A,
                18, 11, 3, 90.0, 50.0, 75.0, 1.25,
                1, 1, 3, 0, 8, 80, 0, 1, 0, 0, 1
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("CLUTCH_MINISTER"));
        assertThat(result.headlineRoast()).contains("won 3 1vX clutches");
    }

    @Test
    @DisplayName("Should assign HEADSHOT_DEMON when kills >= 15 and headshotPct >= 65.0")
    void shouldTriggerHeadshotDemon() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Aimer", MatchTeamSide.TEAM_A,
                22, 10, 2, 105.0, 72.7, 80.0, 1.45,
                3, 1, 1, 0, 12, 100, 0, 2, 0, 1, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("HEADSHOT_DEMON"));
        assertThat(result.headlineRoast()).contains("72.7% headshot accuracy across 22 kills");
    }

    @Test
    @DisplayName("Should assign OPENING_BULLDOZER when firstKills >= 5")
    void shouldTriggerOpeningBulldozer() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Entry", MatchTeamSide.TEAM_A,
                19, 13, 3, 92.0, 55.0, 70.0, 1.30,
                6, 2, 0, 0, 10, 70, 1, 2, 0, 2, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("OPENING_BULLDOZER"));
        assertThat(result.headlineRoast()).contains("Secured 6 opening picks");
    }

    @Test
    @DisplayName("Should assign NADESHOT_CHEF when utilityDamage >= 250")
    void shouldTriggerNadeshotChef() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Chef", MatchTeamSide.TEAM_A,
                15, 14, 7, 80.0, 40.0, 68.0, 1.10,
                1, 2, 1, 0, 15, 290, 1, 1, 0, 1, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("NADESHOT_CHEF"));
        assertThat(result.headlineRoast()).contains("Cooked enemies for 290 utility damage");
    }

    @Test
    @DisplayName("Should assign AWP_CRUTCH when kills >= 12 and awpKills pct >= 60.0")
    void shouldTriggerAwpCrutch() {
        MatchPlayerStatsDto stats = new MatchPlayerStatsDto(
                "76561198000000001", "Sniper", MatchTeamSide.TEAM_A,
                15, 10, 2, 82.0, 25.0, 72.0, 1.15,
                2, 1, 0, 0, 6, 40, 1, 1, 10, 0, 0
        );

        PlayerEvaluationResult result = engine.evaluatePlayer(stats, 20);

        assertThat(result.badges()).anyMatch(b -> b.badgeCode().equals("AWP_CRUTCH"));
        assertThat(result.headlineRoast()).contains("10 of 15 kills with the Big Green Gun");
    }

    @Test
    @DisplayName("Should assign fallback SOLID_SOLDIER or LOBBY_TOURIST when no triggers met")
    void shouldAssignFallbackBadges() {
        // High impact rating -> SOLID_SOLDIER
        MatchPlayerStatsDto solidStats = new MatchPlayerStatsDto(
                "76561198000000001", "Solid", MatchTeamSide.TEAM_A,
                11, 11, 4, 72.0, 45.0, 65.0, 1.05,
                1, 1, 0, 0, 5, 60, 1, 1, 0, 0, 0
        );
        PlayerEvaluationResult solidResult = engine.evaluatePlayer(solidStats, 20);
        assertThat(solidResult.badges()).hasSize(1);
        assertThat(solidResult.headlineTitle()).isEqualTo("Solid Soldier");
        assertThat(solidResult.badges().get(0).badgeCategory()).isEqualTo(BadgeCategory.FLEX);

        // Low impact rating -> LOBBY_TOURIST
        MatchPlayerStatsDto touristStats = new MatchPlayerStatsDto(
                "76561198000000002", "Tourist", MatchTeamSide.TEAM_A,
                7, 12, 2, 58.0, 35.0, 55.0, 0.78,
                1, 1, 0, 0, 3, 30, 2, 0, 0, 0, 0
        );
        PlayerEvaluationResult touristResult = engine.evaluatePlayer(touristStats, 20);
        assertThat(touristResult.badges()).hasSize(1);
        assertThat(touristResult.headlineTitle()).isEqualTo("Lobby Tourist");
        assertThat(touristResult.badges().get(0).badgeCategory()).isEqualTo(BadgeCategory.ROAST);
    }
}
