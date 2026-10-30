package com.cs2hub.demoparser.service;

import com.cs2hub.demoparser.dto.MatchPlayerStatsDto;
import com.cs2hub.demoparser.dto.ParsedMatchPayloadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Cs2DemoParserEngineTest {

    private Cs2DemoParserEngine engine;

    @BeforeEach
    void setUp() {
        engine = new Cs2DemoParserEngine();
    }

    @Test
    @DisplayName("Should generate deterministic match telemetry when no demo file is provided")
    void shouldGenerateDeterministicMatchWhenNoFileProvided() throws IOException {
        UUID matchId = UUID.randomUUID();
        String shareCode = "CSGO-abcde-12345-fghij-67890-klmno";

        ParsedMatchPayloadDto payload = engine.parse(matchId, shareCode, null);

        assertThat(payload).isNotNull();
        assertThat(payload.mapName()).startsWith("de_");
        assertThat(payload.teamAScore() == 13 || payload.teamBScore() == 13).isTrue();
        assertThat(payload.durationSeconds()).isGreaterThan(0);
        assertThat(payload.players()).hasSize(10);

        long teamACount = payload.players().stream().filter(p -> "TEAM_A".equals(p.teamSide())).count();
        long teamBCount = payload.players().stream().filter(p -> "TEAM_B".equals(p.teamSide())).count();
        assertThat(teamACount).isEqualTo(5);
        assertThat(teamBCount).isEqualTo(5);

        for (MatchPlayerStatsDto player : payload.players()) {
            assertThat(player.steamId()).isNotBlank();
            assertThat(player.playerName()).isNotBlank();
            assertThat(player.kills()).isNotNegative();
            assertThat(player.deaths()).isNotNegative();
            assertThat(player.adr()).isPositive();
        }
    }

    @Test
    @DisplayName("Should produce consistent results for identical share code seed")
    void shouldProduceConsistentResultsForIdenticalSeed() throws IOException {
        UUID matchId = UUID.randomUUID();
        String shareCode = "CSGO-CONSISTENT-SEED-TEST";

        ParsedMatchPayloadDto run1 = engine.parse(matchId, shareCode, null);
        ParsedMatchPayloadDto run2 = engine.parse(matchId, shareCode, null);

        assertThat(run1.mapName()).isEqualTo(run2.mapName());
        assertThat(run1.teamAScore()).isEqualTo(run2.teamAScore());
        assertThat(run1.teamBScore()).isEqualTo(run2.teamBScore());
        assertThat(run1.players().getFirst().kills()).isEqualTo(run2.players().getFirst().kills());
    }

    @Test
    @DisplayName("Should parse valid Source 2 demo file with PBDEMS2 header")
    void shouldParseValidDemoFileWithMagicHeader() throws IOException {
        UUID matchId = UUID.randomUUID();
        String shareCode = "CSGO-VALID-HEADER";
        Path tempFile = Files.createTempFile("test-valid-cs2-", ".dem");

        try {
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                out.write(Cs2DemoParserEngine.SOURCE2_DEMO_MAGIC);
                out.write("DEMO_PAYLOAD_BYTES".getBytes());
            }

            ParsedMatchPayloadDto payload = engine.parse(matchId, shareCode, tempFile);

            assertThat(payload).isNotNull();
            assertThat(payload.players()).hasSize(10);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Should throw exception when demo header is invalid")
    void shouldThrowExceptionWhenDemoHeaderIsInvalid() throws IOException {
        UUID matchId = UUID.randomUUID();
        String shareCode = "CSGO-INVALID-HEADER";
        Path tempFile = Files.createTempFile("test-invalid-cs2-", ".dem");

        try {
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                out.write("HL2DEMO\0".getBytes()); // Invalid Source 1 header
            }

            assertThatThrownBy(() -> engine.parse(matchId, shareCode, tempFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PBDEMS2");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Should throw exception when demo file is shorter than 8 bytes")
    void shouldThrowExceptionWhenDemoFileIsTooShort() throws IOException {
        UUID matchId = UUID.randomUUID();
        String shareCode = "CSGO-SHORT";
        Path tempFile = Files.createTempFile("test-short-", ".dem");

        try {
            Files.write(tempFile, new byte[]{1, 2, 3});

            assertThatThrownBy(() -> engine.parse(matchId, shareCode, tempFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("less than 8 bytes");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
