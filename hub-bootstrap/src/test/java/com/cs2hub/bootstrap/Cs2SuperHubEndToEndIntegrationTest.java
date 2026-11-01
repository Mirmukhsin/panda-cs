package com.cs2hub.bootstrap;

import com.cs2hub.identity.dto.UpdateProfileRequestDto;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.WearCondition;
import com.cs2hub.loadouts.dto.GenerateLoadoutRequestDto;
import com.cs2hub.loadouts.dto.SaveLoadoutItemDto;
import com.cs2hub.loadouts.dto.SaveLoadoutRequestDto;
import com.cs2hub.loadouts.dto.StickerPlacementDto;
import com.cs2hub.matches.domain.MatchTeamSide;
import com.cs2hub.matches.dto.MatchPlayerStatsDto;
import com.cs2hub.matches.dto.ParsedMatchPayloadDto;
import com.cs2hub.matches.dto.SubmitMatchRequestDto;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.dto.ReviewClipRequestDto;
import com.cs2hub.minigames.dto.SubmitClipRequestDto;
import com.cs2hub.minigames.dto.SubmitGuessRequestDto;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.dto.CreateStratRoomRequestDto;
import com.cs2hub.loadouts.service.CatalogSyncService;
import com.cs2hub.stratboard.dto.SaveRoomTacticRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIf("isDatabaseAvailable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Cs2SuperHubEndToEndIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private CatalogSyncService catalogSyncService;

    @BeforeEach
    void setUp() {
        if (catalogSyncService != null) {
            catalogSyncService.seedMissingCuratedSkins();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1 — Health & Flyway Multi-Schema Startup verification")
    void test01_healthAndFlywayMultiSchemaStartup() throws Exception {
        // 1. Verify /api/v1/health returns UP with all 5 core modules operational
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.components['hub-identity']").value("UP"))
                .andExpect(jsonPath("$.data.components['hub-loadouts']").value("UP"))
                .andExpect(jsonPath("$.data.components['hub-minigames']").value("UP"))
                .andExpect(jsonPath("$.data.components['hub-stratboard']").value("UP"))
                .andExpect(jsonPath("$.data.components['hub-matches']").value("UP"));

        // 2. Verify that startup seeders populated the PostgreSQL schemas
        Integer skinsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM loadouts.skins", Integer.class);
        assertThat(skinsCount).isNotNull().isGreaterThanOrEqualTo(33);

        Integer stickersCount = jdbcTemplate.queryForObject("SELECT count(*) FROM loadouts.stickers", Integer.class);
        assertThat(stickersCount).isNotNull().isGreaterThanOrEqualTo(10);

        Integer lineupsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stratboard.utility_lineups", Integer.class);
        assertThat(lineupsCount).isNotNull().isGreaterThanOrEqualTo(18);
    }

    @Test
    @Order(2)
    @DisplayName("Test 2 — Identity & Dev-Login Flow (hub-identity)")
    void test02_identityAndDevLoginFlow() throws Exception {
        // 1. Dev-login as USER and ADMIN
        String userToken = obtainUserToken();
        assertThat(userToken).isNotBlank();

        String adminToken = obtainAdminToken();
        assertThat(adminToken).isNotBlank();

        // 2. GET /api/v1/users/me with USER token
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("IntegrationUser"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        // 3. PATCH /api/v1/users/me/profile updating tradeUrl
        String newTradeUrl = "https://steamcommunity.com/tradeoffer/new/?partner=12345678&token=abcdefgh";
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto(null, newTradeUrl);

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tradeUrl").value(newTradeUrl));

        // 4. Verify database persistence by calling GET /api/v1/users/me again
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeUrl").value(newTradeUrl));

        // 5. Test username/password registration
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        com.cs2hub.identity.dto.RegisterRequestDto regReq = new com.cs2hub.identity.dto.RegisterRequestDto(
                "reg_user_" + uniqueSuffix,
                "reg_" + uniqueSuffix + "@cs2hub.com",
                "SecretPass123!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("reg_user_" + uniqueSuffix))
                .andExpect(jsonPath("$.data.user.email").value("reg_" + uniqueSuffix + "@cs2hub.com"));

        // 6. Test login with email & password
        com.cs2hub.identity.dto.LoginRequestDto loginEmailReq = new com.cs2hub.identity.dto.LoginRequestDto(
                "reg_" + uniqueSuffix + "@cs2hub.com",
                "SecretPass123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginEmailReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("reg_user_" + uniqueSuffix));

        // 7. Test login with username & password
        com.cs2hub.identity.dto.LoginRequestDto loginUsernameReq = new com.cs2hub.identity.dto.LoginRequestDto(
                "reg_user_" + uniqueSuffix,
                "SecretPass123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUsernameReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3 — Daily 'Guess the Elo' Full Lifecycle (hub-minigames)")
    void test03_dailyGuessTheEloFullLifecycle() throws Exception {
        jdbcTemplate.update("DELETE FROM minigames.user_guesses");
        String userToken = obtainUserToken();
        String adminToken = obtainAdminToken();

        // 1. Submit 3 clips as USER
        SubmitClipRequestDto clip1Req = new SubmitClipRequestDto(
                "Mirage A Ramp Ace", "https://youtube.com/watch?v=clip1e2e", "de_mirage", 14500, 6
        );
        SubmitClipRequestDto clip2Req = new SubmitClipRequestDto(
                "Inferno Banana Hold", "https://youtube.com/watch?v=clip2e2e", "de_inferno", 18200, 8
        );
        SubmitClipRequestDto clip3Req = new SubmitClipRequestDto(
                "Nuke Ramp 1v2 Clutch", "https://youtube.com/watch?v=clip3e2e", "de_nuke", 9500, 4
        );

        UUID clip1Id = submitClip(userToken, clip1Req);
        UUID clip2Id = submitClip(userToken, clip2Req);
        UUID clip3Id = submitClip(userToken, clip3Req);

        // 2. Approve all 3 clips as ADMIN
        ReviewClipRequestDto approveReq = new ReviewClipRequestDto(ClipStatus.APPROVED, null);
        reviewClip(adminToken, clip1Id, approveReq);
        reviewClip(adminToken, clip2Id, approveReq);
        reviewClip(adminToken, clip3Id, approveReq);

        // 3. Trigger manual generation of today's Daily Challenge as ADMIN
        mockMvc.perform(post("/api/v1/minigames/admin/daily/generate?seedIfEmpty=false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Call GET /api/v1/minigames/daily as USER, assert anti-cheat masking
        MvcResult dailyResult = mockMvc.perform(get("/api/v1/minigames/daily")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds").isArray())
                .andExpect(jsonPath("$.data.rounds[0].actualPremierRating").doesNotExist())
                .andExpect(jsonPath("$.data.rounds[1].actualPremierRating").doesNotExist())
                .andExpect(jsonPath("$.data.rounds[2].actualPremierRating").doesNotExist())
                .andReturn();

        JsonNode dailyRoot = objectMapper.readTree(dailyResult.getResponse().getContentAsString());
        JsonNode roundsArray = dailyRoot.path("data").path("rounds");
        assertThat(roundsArray.size()).isEqualTo(3);

        UUID round1ClipId = UUID.fromString(roundsArray.get(0).path("clipId").asText());
        UUID round2ClipId = UUID.fromString(roundsArray.get(1).path("clipId").asText());
        UUID round3ClipId = UUID.fromString(roundsArray.get(2).path("clipId").asText());

        // 5. Submit 3 guesses via POST /api/v1/minigames/daily/guess
        submitGuess(userToken, round1ClipId, 14000);
        submitGuess(userToken, round2ClipId, 18000);
        MvcResult lastGuessResult = submitGuess(userToken, round3ClipId, 9800);

        JsonNode lastGuessRoot = objectMapper.readTree(lastGuessResult.getResponse().getContentAsString());
        assertThat(lastGuessRoot.path("data").path("pointsAwarded").asInt()).isGreaterThan(0);
        assertThat(lastGuessRoot.path("data").path("completed").asBoolean()).isTrue();
        assertThat(lastGuessRoot.path("data").path("totalChallengeScore").asInt()).isGreaterThan(0);
        assertThat(lastGuessRoot.path("data").path("shareText").asText()).contains("Guess the Elo");

        // 6. Verify GET /api/v1/minigames/leaderboard returns user's enriched username via UserLookupService
        mockMvc.perform(get("/api/v1/minigames/leaderboard?period=DAILY")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries").isArray())
                .andExpect(jsonPath("$.data.entries[0].username").value("IntegrationUser"));
    }

    @Test
    @Order(4)
    @DisplayName("Test 4 — Budget Loadout Generator & Community Upvote Lifecycle (hub-loadouts)")
    void test04_budgetLoadoutGeneratorAndCommunityUpvoteLifecycle() throws Exception {
        String userToken = obtainUserToken();

        // 1. Generate $10.00 RED budget loadout
        GenerateLoadoutRequestDto lowBudgetReq = new GenerateLoadoutRequestDto(
                new BigDecimal("10.00"), SkinColor.RED, "weapon_m4a1_silencer", false
        );

        mockMvc.perform(post("/api/v1/loadouts/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowBudgetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.totalCostUsd").value(org.hamcrest.Matchers.lessThanOrEqualTo(10.00)))
                .andExpect(jsonPath("$.data.themeColor").value("RED"));

        // 2. Generate $60.00 PURPLE budget loadout
        GenerateLoadoutRequestDto midBudgetReq = new GenerateLoadoutRequestDto(
                new BigDecimal("60.00"), SkinColor.PURPLE, "weapon_m4a1_silencer", false
        );

        mockMvc.perform(post("/api/v1/loadouts/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(midBudgetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.totalCostUsd").value(org.hamcrest.Matchers.lessThanOrEqualTo(60.00)))
                .andExpect(jsonPath("$.data.themeColor").value("PURPLE"));

        // 3. Save a community loadout with sticker crafts as USER
        SaveLoadoutItemDto akItem = new SaveLoadoutItemDto(
                "T_RIFLE",
                "skin-ak47-redline",
                WearCondition.FT,
                List.of(new StickerPlacementDto("sticker-titan-holo-katowice-2014", 0, 0.0, 0.0, 0.0, 1.0))
        );

        SaveLoadoutRequestDto saveRequest = new SaveLoadoutRequestDto(
                "Clean Redline Beast Craft",
                SkinColor.RED,
                new BigDecimal("80.00"),
                true,
                List.of(akItem)
        );

        MvcResult saveResult = mockMvc.perform(post("/api/v1/loadouts/community")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Clean Redline Beast Craft"))
                .andExpect(jsonPath("$.data.author.username").value("IntegrationUser"))
                .andReturn();

        JsonNode saveRoot = objectMapper.readTree(saveResult.getResponse().getContentAsString());
        UUID loadoutId = UUID.fromString(saveRoot.path("data").path("id").asText());

        // 4. Toggle upvote via POST /api/v1/loadouts/community/{id}/upvote
        mockMvc.perform(post("/api/v1/loadouts/community/" + loadoutId + "/upvote")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.upvoted").value(true))
                .andExpect(jsonPath("$.data.upvotes").value(1));

        // 5. Verify GET /api/v1/loadouts/community returns the loadout with upvotes == 1 and enriched author
        mockMvc.perform(get("/api/v1/loadouts/community")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(loadoutId.toString()))
                .andExpect(jsonPath("$.data.content[0].upvotes").value(1))
                .andExpect(jsonPath("$.data.content[0].author.username").value("IntegrationUser"));
    }

    @Test
    @Order(5)
    @DisplayName("Test 5 — Stratboard Lineups, Room Creation & Live Playbook Save (hub-stratboard)")
    void test05_stratboardLineupsRoomCreationAndLivePlaybookSave() throws Exception {
        String userToken = obtainUserToken();

        // 1. Query curated utility lineups for de_mirage smokes
        mockMvc.perform(get("/api/v1/stratboard/lineups?mapName=de_mirage&utilityType=SMOKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data[0].mapName").value("de_mirage"))
                .andExpect(jsonPath("$.data[0].utilityType").value("SMOKE"));

        // 2. Create a live collaborative room
        CreateStratRoomRequestDto createRoomReq = new CreateStratRoomRequestDto(
                "de_mirage",
                StratSide.T,
                "IntegrationUser",
                "#3B82F6",
                null
        );

        MvcResult roomResult = mockMvc.perform(post("/api/v1/stratboard/rooms")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoomReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mapName").value("de_mirage"))
                .andExpect(jsonPath("$.data.roomCode").isNotEmpty())
                .andReturn();

        JsonNode roomRoot = objectMapper.readTree(roomResult.getResponse().getContentAsString());
        String roomCode = roomRoot.path("data").path("roomCode").asText();
        assertThat(roomCode).hasSize(8);

        // 3. Save active room as persistent tactic playbook
        SaveRoomTacticRequestDto saveTacticReq = new SaveRoomTacticRequestDto(
                "Mirage Fast A Execute",
                "Execute through A palace and ramp with 3 smokes",
                true
        );

        MvcResult saveTacticResult = mockMvc.perform(post("/api/v1/stratboard/rooms/" + roomCode + "/save-tactic")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveTacticReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Mirage Fast A Execute"))
                .andExpect(jsonPath("$.data.mapName").value("de_mirage"))
                .andExpect(jsonPath("$.data.shareCode").isNotEmpty())
                .andReturn();

        JsonNode tacticRoot = objectMapper.readTree(saveTacticResult.getResponse().getContentAsString());
        String shareCode = tacticRoot.path("data").path("shareCode").asText();
        assertThat(shareCode).hasSize(8);

        // 4. Retrieve tactic playbook by its 8-character shareCode
        mockMvc.perform(get("/api/v1/stratboard/tactics/" + shareCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareCode").value(shareCode))
                .andExpect(jsonPath("$.data.title").value("Mirage Fast A Execute"))
                .andExpect(jsonPath("$.data.mapName").value("de_mirage"))
                .andExpect(jsonPath("$.data.author.username").value("IntegrationUser"));
    }

    @Test
    @Order(6)
    @DisplayName("Test 6 — Match Analyzer Simulation, Worker Claim/Complete & Roast Card (hub-matches)")
    void test06_matchAnalyzerSimulationWorkerClaimCompleteAndRoastCard() throws Exception {
        String userToken = obtainUserToken();

        // 1. Simulate an analyzed match
        mockMvc.perform(post("/api/v1/matches/simulate?mapName=de_mirage")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mapName").value("de_mirage"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.teamAPlayers").isArray())
                .andExpect(jsonPath("$.data.teamAPlayers.length()").value(5))
                .andExpect(jsonPath("$.data.teamBPlayers").isArray())
                .andExpect(jsonPath("$.data.teamBPlayers.length()").value(5))
                .andExpect(jsonPath("$.data.teamAPlayers[0].awards").isArray());

        // 2. Submit a queued match for demo parsing
        String testShareCode = "CSGO-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SubmitMatchRequestDto submitReq = new SubmitMatchRequestDto(
                testShareCode, "https://demos.cs2hub.com/sample.dem"
        );

        MvcResult submitResult = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andReturn();

        JsonNode submitRoot = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        UUID queuedMatchId = UUID.fromString(submitRoot.path("data").path("id").asText());

        // 3. Worker claims next match job with X-Worker-Secret
        mockMvc.perform(post("/api/v1/internal/matches/claim-next")
                        .header("X-Worker-Secret", "dev-worker-secret-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value(queuedMatchId.toString()))
                .andExpect(jsonPath("$.data.shareCode").value(testShareCode));

        // 4. Worker completes match parsing with 10 players payload
        List<MatchPlayerStatsDto> players = new ArrayList<>();
        String targetSteamId = "76561198000000010";

        // Add 5 players for Team A
        players.add(new MatchPlayerStatsDto(
                targetSteamId, "TopFragger", MatchTeamSide.TEAM_A,
                28, 10, 6, 115.4, 64.0, 85.0, 1.65, 5, 1, 3, 0, 12, 210, 0, 4, 15, 2, 1
        ));
        for (int i = 1; i < 5; i++) {
            players.add(new MatchPlayerStatsDto(
                    "7656119800000001" + i, "TeamAPlayer" + i, MatchTeamSide.TEAM_A,
                    15, 12, 4, 75.0, 50.0, 70.0, 1.05, 2, 2, 1, 1, 8, 120, 1, 2, 0, 1, 0
            ));
        }

        // Add 5 players for Team B
        for (int i = 5; i < 10; i++) {
            players.add(new MatchPlayerStatsDto(
                    "7656119800000001" + i, "TeamBPlayer" + i, MatchTeamSide.TEAM_B,
                    11, 17, 3, 58.0, 42.0, 60.0, 0.85, 1, 3, 0, 4, 6, 80, 4, 1, 0, 0, 0
            ));
        }

        ParsedMatchPayloadDto parsedPayload = new ParsedMatchPayloadDto(
                "de_mirage", 13, 8, 2100, players
        );

        mockMvc.perform(post("/api/v1/internal/matches/" + queuedMatchId + "/complete")
                        .header("X-Worker-Secret", "dev-worker-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parsedPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.teamAPlayers").isArray())
                .andExpect(jsonPath("$.data.teamAPlayers.length()").value(5))
                .andExpect(jsonPath("$.data.teamBPlayers").isArray())
                .andExpect(jsonPath("$.data.teamBPlayers.length()").value(5));

        // 5. Fetch shareable player roast card
        mockMvc.perform(get("/api/v1/matches/" + queuedMatchId + "/players/" + targetSteamId + "/roast-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerName").value("TopFragger"))
                .andExpect(jsonPath("$.data.steamId").value(targetSteamId))
                .andExpect(jsonPath("$.data.badges").isArray())
                .andExpect(jsonPath("$.data.shareSlug").isNotEmpty());
    }

    private UUID submitClip(String token, SubmitClipRequestDto request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/minigames/clips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(root.path("data").path("id").asText());
    }

    private void reviewClip(String adminToken, UUID clipId, ReviewClipRequestDto request) throws Exception {
        mockMvc.perform(patch("/api/v1/minigames/admin/clips/" + clipId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private MvcResult submitGuess(String token, UUID clipId, int guessedPremierRating) throws Exception {
        SubmitGuessRequestDto guessReq = new SubmitGuessRequestDto(clipId, guessedPremierRating);
        return mockMvc.perform(post("/api/v1/minigames/daily/guess")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guessReq)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
