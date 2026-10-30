package com.cs2hub.demoparser.service;

import com.cs2hub.demoparser.dto.MatchPlayerStatsDto;
import com.cs2hub.demoparser.dto.ParsedMatchPayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class Cs2DemoParserEngine {

    public static final byte[] SOURCE2_DEMO_MAGIC = new byte[]{'P', 'B', 'D', 'E', 'M', 'S', '2', 0x00};

    private static final String[] MAP_POOL = {
            "de_mirage", "de_inferno", "de_dust2", "de_nuke", "de_ancient", "de_anubis"
    };

    private static final String[] ARCHETYPE_NAMES_A = {
            "s1mple_fan", "ZywOo_peek", "m0NESY_flick", "b1t_headshot", "Aleksib_tactics"
    };

    private static final String[] ARCHETYPE_NAMES_B = {
            "donk_aggro", "sh1ro_save", "chopper_igf", "magixx_anchor", "zonixx_clutch"
    };

    public ParsedMatchPayloadDto parse(UUID matchId, String shareCode, Path demoFile) throws IOException {
        long seed;
        String effectiveShareCode = shareCode != null && !shareCode.isBlank() ? shareCode : "CSGO-AUTO-" + matchId;

        if (demoFile != null) {
            validateDemoHeader(demoFile);
            seed = effectiveShareCode.hashCode() ^ Files.size(demoFile);
            log.info("Validated PBDEMS2 header for demo file: {}", demoFile);
        } else {
            seed = effectiveShareCode.hashCode();
            log.info("No demo file provided; generating deterministic match telemetry for share code: {}", effectiveShareCode);
        }

        return generateMatchPayload(seed);
    }

    private void validateDemoHeader(Path demoFile) throws IOException {
        if (!Files.exists(demoFile)) {
            throw new IllegalArgumentException("Demo file does not exist: " + demoFile);
        }
        if (Files.size(demoFile) < 8) {
            throw new IllegalArgumentException("Corrupt demo file: size is less than 8 bytes");
        }

        byte[] header = new byte[8];
        try (InputStream in = Files.newInputStream(demoFile)) {
            int read = in.read(header);
            if (read < 8 || !Arrays.equals(header, SOURCE2_DEMO_MAGIC)) {
                throw new IllegalArgumentException("Invalid CS2 demo replay: missing PBDEMS2 Source 2 magic header");
            }
        }
    }

    private ParsedMatchPayloadDto generateMatchPayload(long seed) {
        Random random = new Random(seed);

        String mapName = MAP_POOL[Math.abs(random.nextInt()) % MAP_POOL.length];
        boolean teamAWins = random.nextBoolean();
        int winnerScore = 13;
        int loserScore = 4 + random.nextInt(8); // 4 to 11
        int teamAScore = teamAWins ? winnerScore : loserScore;
        int teamBScore = teamAWins ? loserScore : winnerScore;
        int totalRounds = teamAScore + teamBScore;
        int durationSeconds = totalRounds * (85 + random.nextInt(25));

        List<MatchPlayerStatsDto> players = new ArrayList<>();

        // Generate 5 Team A players
        for (int i = 0; i < 5; i++) {
            String steamId = String.format("765611980000000%02d", (i + 1));
            String name = ARCHETYPE_NAMES_A[i];
            players.add(generatePlayer(random, steamId, name, "TEAM_A", i, teamAWins, totalRounds));
        }

        // Generate 5 Team B players
        for (int i = 0; i < 5; i++) {
            String steamId = String.format("765611980000000%02d", (i + 6));
            String name = ARCHETYPE_NAMES_B[i];
            players.add(generatePlayer(random, steamId, name, "TEAM_B", i, !teamAWins, totalRounds));
        }

        return new ParsedMatchPayloadDto(mapName, teamAScore, teamBScore, durationSeconds, players);
    }

    private MatchPlayerStatsDto generatePlayer(
            Random random,
            String steamId,
            String name,
            String teamSide,
            int roleIndex,
            boolean wonMatch,
            int totalRounds
    ) {
        int kills;
        int deaths;
        int assists = 2 + random.nextInt(6);
        double adr;
        double headshotPct = Math.round((35.0 + random.nextDouble() * 30.0) * 10.0) / 10.0;
        double kastPct;
        double impactRating;
        int firstKills = 1 + random.nextInt(4);
        int firstDeaths = 1 + random.nextInt(4);
        int clutchesWon = 0;
        int teamFlashes = random.nextInt(3);
        int enemiesFlashed = 5 + random.nextInt(15);
        int utilityDamage = 60 + random.nextInt(200);
        int zeroDamageRounds = random.nextInt(3);
        int ecoKills = 1 + random.nextInt(4);
        int awpKills = 0;
        int bombPlants = random.nextInt(3);
        int bombDefuses = random.nextInt(2);

        if (roleIndex == 0) {
            // Star Fragger / Carry Archetype
            kills = 24 + random.nextInt(7);
            deaths = Math.max(8, totalRounds / 2 - random.nextInt(4));
            adr = Math.round((95.0 + random.nextDouble() * 20.0) * 10.0) / 10.0;
            kastPct = Math.round((75.0 + random.nextDouble() * 15.0) * 10.0) / 10.0;
            impactRating = Math.round((1.40 + random.nextDouble() * 0.40) * 100.0) / 100.0;
            clutchesWon = 2 + random.nextInt(2);
            firstKills = 5 + random.nextInt(3);
            firstDeaths = 1 + random.nextInt(2);
        } else if (roleIndex == 1) {
            // Sniper / AWPer Archetype
            kills = 18 + random.nextInt(6);
            deaths = Math.max(10, totalRounds / 2);
            awpKills = 12 + random.nextInt(6);
            adr = Math.round((80.0 + random.nextDouble() * 15.0) * 10.0) / 10.0;
            kastPct = Math.round((70.0 + random.nextDouble() * 10.0) * 10.0) / 10.0;
            impactRating = Math.round((1.15 + random.nextDouble() * 0.25) * 100.0) / 100.0;
        } else if (roleIndex == 2) {
            // Support / Utility Specialist Archetype
            kills = 12 + random.nextInt(5);
            deaths = Math.max(12, totalRounds / 2 + 1);
            utilityDamage = 280 + random.nextInt(150);
            enemiesFlashed = 18 + random.nextInt(10);
            teamFlashes = 6 + random.nextInt(4); // Trigger Team Flash King candidate
            bombPlants = 4 + random.nextInt(2);  // Bomb Magnet candidate
            bombDefuses = wonMatch ? 2 : 0;
            adr = Math.round((65.0 + random.nextDouble() * 12.0) * 10.0) / 10.0;
            kastPct = Math.round((68.0 + random.nextDouble() * 10.0) * 10.0) / 10.0;
            impactRating = Math.round((0.85 + random.nextDouble() * 0.20) * 100.0) / 100.0;
        } else if (roleIndex == 3) {
            // Aggressive Entry Fragger (High First Deaths candidate)
            kills = 14 + random.nextInt(6);
            deaths = totalRounds - 3 - random.nextInt(3);
            firstKills = 2 + random.nextInt(2);
            firstDeaths = 6 + random.nextInt(3); // Trigger First Contact
            adr = Math.round((70.0 + random.nextDouble() * 15.0) * 10.0) / 10.0;
            kastPct = Math.round((60.0 + random.nextDouble() * 10.0) * 10.0) / 10.0;
            impactRating = Math.round((0.95 + random.nextDouble() * 0.20) * 100.0) / 100.0;
        } else {
            // Struggling / Ghost Operator Candidate
            kills = 5 + random.nextInt(4);
            deaths = totalRounds - 2;
            adr = Math.round((40.0 + random.nextDouble() * 15.0) * 10.0) / 10.0;
            kastPct = Math.round((50.0 + random.nextDouble() * 10.0) * 10.0) / 10.0;
            impactRating = Math.round((0.55 + random.nextDouble() * 0.20) * 100.0) / 100.0;
            zeroDamageRounds = 4 + random.nextInt(3); // Trigger Ghost Operator
        }

        return new MatchPlayerStatsDto(
                steamId,
                name,
                teamSide,
                kills,
                deaths,
                assists,
                adr,
                headshotPct,
                kastPct,
                impactRating,
                firstKills,
                firstDeaths,
                clutchesWon,
                teamFlashes,
                enemiesFlashed,
                utilityDamage,
                zeroDamageRounds,
                ecoKills,
                awpKills,
                bombPlants,
                bombDefuses
        );
    }
}
