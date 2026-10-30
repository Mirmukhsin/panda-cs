# CS2 SuperHub — Master Frontend & Microservice API Contracts

Welcome to the **CS2 SuperHub** API Contracts directory. This repository contains the source-of-truth API and WebSocket contracts designed for Frontend Engineers (React / Next.js / Vue / Mobile) and external microservice integration.

---

## 1. Domain Specification Index

| Domain / Module | Contract Specification | Service Scope & Highlights | Auth Model |
|---|---|---|---|
| **Identity & Users** | [`identity-api.md`](./identity-api.md) | Steam OpenID 2.0 authentication, dev mock login, JWT token issuance (`HS256`), and user profile management (`identity` schema). | Public & JWT Bearer |
| **Minigames & Daily Challenges** | [`minigames-api.md`](./minigames-api.md) | Daily "Guess the Elo" Premier rating challenge, clip submission, moderation queue, daily cron rotation, streak tracking, and global leaderboards (`minigames` schema). | Public & JWT Bearer |
| **Loadouts & Crafting** | [`loadouts-api.md`](./loadouts-api.md) | CS2 skin and sticker catalog sync, Color-Matched Budget Loadout Generator algorithm (Greedy knapsack with color bonus), community loadout creation, upvote deduplication, and sticker crafts (`loadouts` schema). | Public & JWT Bearer |
| **Stratboard & Tactics** | [`stratboard-api.md`](./stratboard-api.md) | Curated CS2 utility lineup library (smokes, flashes, molotovs), saved tactical playbooks, and real-time collaborative 2D map tactical rooms via Spring WebSocket (STOMP over SockJS) (`stratboard` schema). | Public, JWT Bearer & STOMP WS |
| **Matches & Roast Engine** | [`matches-api.md`](./matches-api.md) | Automated "Roast & Flex" Match Analyzer, Valve share code ingestion, developer match simulator (`/simulate`), shareable viral player roast cards, and replay parsing daemon integration (`matches` schema). | Public, JWT Bearer & Worker Secret (`X-Worker-Secret`) |

---

## 2. Infrastructure & Environment Configurations

### Server Ports & Base URLs
- **Monolith API Gateway / Web Services (`hub-bootstrap`):**
  - Base URL: `http://localhost:8080`
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - OpenAPI 3.0 JSON: `http://localhost:8080/v3/api-docs`
  - WebSocket STOMP Endpoint: `ws://localhost:8080/ws/stratboard` (with SockJS fallback at `http://localhost:8080/ws/stratboard`)
- **Demo Parser Microservice Daemon (`worker-demoparser`):**
  - Base URL: `http://localhost:8081`

### Authentication Standards
- **User JWT Bearer Authentication:**
  - Header: `Authorization: Bearer <jwt-token>`
  - Included claims: `sub` (Steam ID 64), `userId` (Platform UUID), `role` (`USER`, `MODERATOR`, `ADMIN`).
- **Internal Worker Daemon Authentication:**
  - Header: `X-Worker-Secret: ${WORKER_SHARED_SECRET:dev-worker-secret-key}`
- **Standard API Envelope (`ApiResponseDto<T>`):**
  All REST endpoints return payloads structured as:
  ```json
  {
    "success": true,
    "message": "Operation description",
    "data": { ... },
    "timestamp": "2026-10-30T12:00:00Z"
  }
  ```

---

## 3. Quick Local Development Onboarding

1. **Development Login Helper:**
   Send `POST /api/v1/auth/dev-login` with `{ "steamId": "76561198000000001", "username": "DevPlayer", "role": "ADMIN" }` to instantly receive a valid JWT token without needing Steam OpenID redirection.
2. **Instant Match Simulation:**
   Send `POST /api/v1/matches/simulate` to generate a fully populated CS2 match with 10 players, statistics, and Roast & Flex awards for immediate frontend UI layout development.
3. **Interactive Stratboard Canvas:**
   Connect via STOMP client (e.g. `@stomp/stompjs` in React) to `/ws/stratboard` and subscribe to `/topic/rooms/{roomCode}` to broadcast tactical annotations and sync coordinates in real time.
