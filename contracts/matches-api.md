# CS2 SuperHub — Matches & "Roast & Flex" Match Analyzer API Contract

**Module:** `hub-matches` & `worker-demoparser`  
**Base URL:** `http://localhost:8080` (Main API) / `http://localhost:8081` (Demo Parser Daemon)  
**Specification Version:** `v1.0.0`

---

## 1. Overview & Architectural Design

The **Matches** domain delivers the automated **"Roast & Flex"** CS2 Match Analyzer. It solves the friction of competitive replay review by turning raw 64-tick and 128-tick match telemetries into viral, shareable player report cards with signature community awards.

### Core Architecture Components
1. **Match Ingestion & Queue (`hub-matches`):**
   - Accepts Valve CS2 34-character Share Codes (`CSGO-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx`) or direct `.dem` replay URLs.
   - Idempotently prevents duplicate parsing if a match share code has already been registered.
   - Provides public 8-character URL slugs (`shareSlug`) for instant sharing on Discord, Reddit, or Twitter.
2. **Dedicated Replay Worker (`worker-demoparser`):**
   - Lightweight standalone Spring Boot microservice running on port `8081`.
   - Has zero database coupling (no JPA, no Flyway, no PostgreSQL).
   - Polls `hub-matches` for queued jobs via `/api/v1/internal/matches/claim-next` secured by an `X-Worker-Secret` header.
   - Enforces strict disk hygiene: downloads `.dem` replays to ephemeral temp files, verifies the Source 2 `PBDEMS2` magic header, parses player telemetries (or generates deterministic fallback telemetry when only a share code is supplied), and **strictly deletes all temporary files in a `finally` block**.
3. **Deterministic "Roast & Flex" Badge Engine (`RoastBadgeEngine`):**
   - Automatically computes 10 signature community badges plus 2 fallbacks across `FLEX`, `TROLL`, `UTILITY`, and `NEUTRAL` categories.
   - Generates contextual social media copy (e.g., *"Carried the team on Mirage with 27 kills and 1.54 impact"* or *"Zero opening duels and 60 ADR while saving all game"*).
4. **Developer Match Simulator (`/simulate`):**
   - Allows instant creation of fully populated matches with 10 archetype players (Star Fragger, AWPer, Utility Specialist, Baiter, etc.) without requiring external demo downloads or waiting for background workers.

---

## 2. Public & Community Match APIs

Base path: `/api/v1/matches`

### 2.1. Submit Match for Analysis
- **Method:** `POST /api/v1/matches/submit`
- **Auth:** Optional (if authenticated, `submittedBy` will link to the user's profile)
- **Description:** Submits a CS2 match share code or replay URL. If the share code has already been processed or queued, the existing record is returned without re-queuing.
- **Request Headers:**
  - `Authorization: Bearer <jwt-token>` (optional)
  - `Content-Type: application/json`
- **Request Body:**
```json
{
  "shareCode": "CSGO-abcde-12345-fghij-67890-klmno",
  "demoUrl": "https://replay138.valve.net/730/003712345678901234567_1234567890.dem.bz2"
}
```
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Match queued for processing",
  "data": {
    "id": "c1f7b029-d5c4-4d89-9a12-8ef7321a4bc3",
    "shareCode": "CSGO-abcde-12345-fghij-67890-klmno",
    "shareSlug": "k9x2m4p1",
    "mapName": "de_mirage",
    "status": "QUEUED",
    "teamAScore": 0,
    "teamBScore": 0,
    "durationSeconds": 0,
    "createdAt": "2026-10-30T15:30:00Z"
  },
  "timestamp": "2026-10-30T15:30:00Z"
}
```

---

### 2.2. Simulate Match (Instant Frontend Prototyping)
- **Method:** `POST /api/v1/matches/simulate`
- **Auth:** Optional
- **Description:** Instantly creates and completes a match with 10 archetype players and pre-calculated badges. Ideal for local development, UI testing, and presentation demos.
- **Request Body (all fields optional):**
```json
{
  "shareCode": "CSGO-SIM-DEV-001",
  "mapName": "de_mirage"
}
```
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Match simulated and analyzed successfully",
  "data": {
    "id": "a90184e2-7634-4b51-912f-c93d8b1390ea",
    "shareCode": "CSGO-SIM-DEV-001",
    "shareSlug": "sim-a901",
    "mapName": "de_mirage",
    "status": "COMPLETED",
    "teamAScore": 13,
    "teamBScore": 8,
    "durationSeconds": 2185,
    "submittedBy": {
      "id": "993a4b12-9c10-47de-84d2-f67341258190",
      "username": "s1mple_pro",
      "avatarUrl": "https://avatars.steamstatic.com/avatar.jpg"
    },
    "players": [
      {
        "id": "3b291048-c812-4d2a-8d19-48201fa84301",
        "steamId": "76561198000000001",
        "playerName": "The_Carry_God",
        "teamSide": "TEAM_A",
        "kills": 28,
        "deaths": 11,
        "assists": 4,
        "adr": 104.5,
        "headshotPct": 57.1,
        "kastPct": 81.8,
        "impactRating": 1.62,
        "firstKills": 6,
        "firstDeaths": 1,
        "clutchesWon": 2,
        "teamFlashes": 0,
        "enemiesFlashed": 9,
        "utilityDamage": 120,
        "zeroDamageRounds": 1,
        "ecoKills": 3,
        "awpKills": 4,
        "bombPlants": 2,
        "bombDefuses": 1,
        "awards": [
          {
            "badge": "THE_CARRY",
            "displayName": "The Backpack",
            "category": "FLEX",
            "emoji": "🎒",
            "description": "Hard-carried the lobby with 28 frags and 1.62 impact rating."
          },
          {
            "badge": "MR_CLUTCH",
            "displayName": "Ice in Veins",
            "category": "FLEX",
            "emoji": "🧊",
            "description": "Won 2 1vX clutch rounds under maximum pressure."
          }
        ]
      }
    ],
    "createdAt": "2026-10-30T15:31:00Z"
  },
  "timestamp": "2026-10-30T15:31:00Z"
}
```

---

### 2.3. Get Match Scoreboard & Analysis by ID
- **Method:** `GET /api/v1/matches/{matchId}`
- **Auth:** Public
- **Path Parameters:**
  - `matchId` (UUID, required): Match analysis UUID
- **Response `200 OK`:** Returns `MatchAnalysisDetailDto` with full player scoreboard, damage stats, and badge breakdowns.

---

### 2.4. Get Match Scoreboard by Share Slug
- **Method:** `GET /api/v1/matches/slug/{shareSlug}`
- **Auth:** Public
- **Path Parameters:**
  - `shareSlug` (string, required): 8-character public share slug (e.g. `k9x2m4p1`)
- **Response `200 OK`:** Returns `MatchAnalysisDetailDto`.

---

### 2.5. Get Shareable Player Roast Card
- **Method:** `GET /api/v1/matches/{matchId}/players/{steamId}/roast`
- **Auth:** Public
- **Description:** Generates a viral social media card payload for an individual player in a match, highlighting their primary award and roast quote.
- **Path Parameters:**
  - `matchId` (UUID, required): Match UUID
  - `steamId` (string, required): Player's 64-bit Steam ID
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "matchId": "a90184e2-7634-4b51-912f-c93d8b1390ea",
    "steamId": "76561198000000002",
    "playerName": "SneakyBaiter",
    "teamSide": "TEAM_A",
    "mapName": "de_mirage",
    "kills": 12,
    "deaths": 9,
    "adr": 58.4,
    "headshotPct": 41.6,
    "kastPct": 77.2,
    "impactRating": 0.72,
    "primaryBadge": {
      "badge": "BAIT_LORD",
      "displayName": "Master Baiter",
      "category": "TROLL",
      "emoji": "🎣",
      "description": "Surviving with 9 deaths and zero opening deaths while the team took all the fire."
    },
    "allAwards": [
      {
        "badge": "BAIT_LORD",
        "displayName": "Master Baiter",
        "category": "TROLL",
        "emoji": "🎣",
        "description": "Surviving with 9 deaths and zero opening deaths while the team took all the fire."
      }
    ],
    "roastQuote": "SneakyBaiter took the baiting masterclass: 9 deaths and 0 opening duels, watching teammates die from a safe distance.",
    "shareUrl": "https://cs2hub.gg/m/sim-a901?p=76561198000000002"
  },
  "timestamp": "2026-10-30T15:32:00Z"
}
```

---

### 2.6. Get Player Match History
- **Method:** `GET /api/v1/matches/history/{steamId}`
- **Auth:** Public
- **Path Parameters:**
  - `steamId` (string, required): Player's 64-bit Steam ID
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "matchId": "a90184e2-7634-4b51-912f-c93d8b1390ea",
      "shareSlug": "sim-a901",
      "mapName": "de_mirage",
      "teamSide": "TEAM_A",
      "teamAScore": 13,
      "teamBScore": 8,
      "won": true,
      "kills": 28,
      "deaths": 11,
      "adr": 104.5,
      "impactRating": 1.62,
      "primaryBadge": {
        "badge": "THE_CARRY",
        "displayName": "The Backpack",
        "category": "FLEX",
        "emoji": "🎒",
        "description": "Hard-carried the lobby with 28 frags and 1.62 impact rating."
      },
      "playedAt": "2026-10-30T15:31:00Z"
    }
  ],
  "timestamp": "2026-10-30T15:33:00Z"
}
```

---

## 3. Internal Replay Worker Callbacks API

Base path: `/api/v1/internal/matches`  
**Authentication:** Secured via HTTP header `X-Worker-Secret: ${WORKER_SHARED_SECRET}`. Requests without this header or with an invalid secret return `401 Unauthorized`.

### 3.1. Claim Next Queued Match
- **Method:** `POST /api/v1/internal/matches/claim-next`
- **Description:** Atomically retrieves the oldest match with status `QUEUED` and transitions it to `PARSING`. If no matches are queued, returns `data: null`.
- **Headers:**
  - `X-Worker-Secret: <shared-secret>`
- **Response `200 OK` (Job Available):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "matchId": "c1f7b029-d5c4-4d89-9a12-8ef7321a4bc3",
    "shareCode": "CSGO-abcde-12345-fghij-67890-klmno",
    "demoUrl": "https://replay138.valve.net/730/003712345678901234567_1234567890.dem.bz2",
    "shareSlug": "k9x2m4p1"
  },
  "timestamp": "2026-10-30T15:35:00Z"
}
```
- **Response `200 OK` (Queue Empty):**
```json
{
  "success": true,
  "message": "Success",
  "data": null,
  "timestamp": "2026-10-30T15:35:00Z"
}
```

---

### 3.2. Complete Match Parsing
- **Method:** `POST /api/v1/internal/matches/{matchId}/complete`
- **Description:** Called by `worker-demoparser` after successfully parsing a replay. Delivers player telemetries, runs the Roast & Flex badge algorithm, persists players and awards, and transitions status to `COMPLETED`.
- **Headers:**
  - `X-Worker-Secret: <shared-secret>`
  - `Content-Type: application/json`
- **Path Parameters:**
  - `matchId` (UUID, required): Match UUID
- **Request Body:**
```json
{
  "mapName": "de_mirage",
  "teamAScore": 13,
  "teamBScore": 9,
  "durationSeconds": 2340,
  "players": [
    {
      "steamId": "76561198000000001",
      "playerName": "s1mple_fan",
      "teamSide": "TEAM_A",
      "kills": 27,
      "deaths": 12,
      "assists": 4,
      "adr": 102.4,
      "headshotPct": 59.2,
      "kastPct": 81.8,
      "impactRating": 1.55,
      "firstKills": 5,
      "firstDeaths": 1,
      "clutchesWon": 2,
      "teamFlashes": 0,
      "enemiesFlashed": 11,
      "utilityDamage": 140,
      "zeroDamageRounds": 0,
      "ecoKills": 4,
      "awpKills": 6,
      "bombPlants": 2,
      "bombDefuses": 1
    }
  ]
}
```
- **Response `200 OK`:** Returns `MatchAnalysisDetailDto` with awarded badges.

---

### 3.3. Report Match Parsing Failure
- **Method:** `POST /api/v1/internal/matches/{matchId}/fail`
- **Description:** Called when replay download fails, header is corrupt, or parsing times out. Records failure reason and transitions status to `FAILED`.
- **Headers:**
  - `X-Worker-Secret: <shared-secret>`
  - `Content-Type: application/json`
- **Request Body:**
```json
{
  "reason": "Invalid CS2 demo replay: missing PBDEMS2 Source 2 magic header"
}
```
- **Response `200 OK`:** Returns `MatchAnalysisDetailDto` in `FAILED` status.

---

## 4. "Roast & Flex" Badge Catalog & Rules

| Badge Key | Display Name | Category | Emoji | Unlock Conditions | Roast / Flex Copy |
|---|---|---|---|---|---|
| `THE_CARRY` | The Backpack | `FLEX` | 🎒 | `kills >= 25 && impact >= 1.40 && adr >= 95.0` | Carrying with frags and impact. |
| `BAIT_LORD` | Master Baiter | `TROLL` | 🎣 | `deaths <= 10 && adr < 65.0 && firstDeaths == 0 && kast >= 75.0` | Surviving while letting teammates take all fire. |
| `ECO_WARRIOR` | Eco Cobra | `TROLL` | 🐍 | `ecoKills >= 5 && (ecoKills / kills) >= 0.30` | Padding stats strictly during anti-eco rounds against unarmored opponents. |
| `AWP_CRUTCH` | The Crutch | `FLEX` | 🔭 | `awpKills >= 12 && (awpKills / kills) >= 0.65` | Cannot hit anything with rifles; one-shot body shot specialist. |
| `MR_CLUTCH` | Ice in Veins | `FLEX` | 🧊 | `clutchesWon >= 2` | Cold-blooded 1vX clutch winner. |
| `DIFFUSE_OR_DIE`| Bomb Whisperer | `UTILITY` | ✂️ | `bombDefuses >= 2` | Clutch defuse maestro when the clock was ticking down. |
| `TEAM_FLASH_KING` | Flashbang Fiend | `TROLL` | 🙈 | `teamFlashes >= 5 && teamFlashes > enemiesFlashed * 0.35` | Blinded own teammates more often than enemies. |
| `BOMB_MAGNET` | C4 Courier | `UTILITY` | 💣 | `bombPlants >= 4` | Dedicated objective player constantly planting the bomb. |
| `FIRST_CONTACT` | First Blood Victim | `TROLL` | 🎯 | `(firstKills + firstDeaths) >= 7 && firstDeaths > firstKills` | Running into crosshairs every single round start. |
| `GHOST_OPERATOR` | AFK Spectator | `TROLL` | 👻 | `zeroDamageRounds >= 4 && kills <= 8` | Deals zero damage in multiple rounds; basically an expensive spectator. |
| `SOLID_SOLDIER` | Solid Contributor | `NEUTRAL` | 🛡️ | `kills >= 15 && adr >= 70.0` (fallback) | Balanced performance supporting team round wins. |
| `LOBBY_TOURIST` | Sightseer | `NEUTRAL` | 📸 | Default fallback | Enjoyed the virtual scenery around the map. |

---

## 5. Error Code Reference

| Status Code | Message Key | Description |
|---|---|---|
| `400 BAD_REQUEST` | Validation error | Missing required fields or invalid share code syntax |
| `401 UNAUTHORIZED` | Invalid worker secret | Provided `X-Worker-Secret` did not match configured value |
| `404 NOT_FOUND` | Match not found | Specified `matchId` or `shareSlug` does not exist |
| `404 NOT_FOUND` | Player not in match | Specified `steamId` did not play in this match |
| `409 CONFLICT` | Match already completed | Attempted to process a match that has already been finalized |
