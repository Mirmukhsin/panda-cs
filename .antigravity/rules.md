# CS2-SuperHub Architecture & Workspace Rules

## Core Architectural Style: Strict Modular Monolith
The `cs2-superhub` backend is designed as a strict Modular Monolith in Java 21 and Spring Boot 3.4+. All developers and AI agents working on this repository must adhere to the boundaries defined below.

---

### 1. Module Inventory & Responsibilities
The repository is split into 7 Maven modules under `com.cs2hub`:
- `hub-common`: Shared foundation utilities, base DTOs, security context helpers, JWT infrastructure, global error handling, OpenAPI config, and cross-module SPI contracts.
- `hub-identity`: Steam OpenID 2.0 authentication, JWT issuance, and user profile management (`identity` schema).
- `hub-minigames`: Browser-based daily CS2 minigames like "Guess the Elo" from video clips, daily cron challenges, and leaderboards (`minigames` schema) — NOT CS2 game server hosting.
- `hub-loadouts`: CS2 skin & sticker catalog sync (`ByMykel/CSGO-API`), skin price tracking, Budget Loadout Generator, and community sticker crafts (`loadouts` schema).
- `hub-stratboard`: Real-time collaborative 2D map tactical canvas (Spring WebSockets/STOMP) and utility lineup playbooks (`stratboard` schema).
- `hub-matches`: CS2 `.dem` match demo analysis, "Roast & Flex" automated badge/award calculation, and shareable match summary cards (`matches` schema).
- `hub-bootstrap`: Application entry point (`@SpringBootApplication`), centralized configuration, Flyway migration runner, and runtime orchestration.

---

### 2. Strict Boundary Enforcement Rules

#### RULE 1: NO Cross-Module Entity or Repository Imports
- A feature module (`hub-identity`, `hub-loadouts`, `hub-minigames`, `hub-stratboard`, `hub-matches`) must **NEVER** import a JPA `@Entity` or `@Repository` from any other feature module.
- Feature modules depend **only** on `hub-common`. They must not declare Maven dependencies on sibling feature modules.
- Inter-module communication must happen via:
  1. Application domain events (`ApplicationEventPublisher` / `@ApplicationModuleListener` or Spring event listeners).
  2. Public API interfaces exposed in `com.cs2hub.<module>.api` with record DTOs.
- `hub-bootstrap` is the only module that imports all feature modules for dependency injection assembly.

#### RULE 2: NO Cross-Schema SQL Joins & Schema Segregation
- Each domain module strictly owns its dedicated PostgreSQL schema:
  - `identity` schema -> owned exclusively by `hub-identity`
  - `loadouts` schema -> owned exclusively by `hub-loadouts`
  - `minigames` schema -> owned exclusively by `hub-minigames`
  - `stratboard` schema -> owned exclusively by `hub-stratboard`
  - `matches` schema -> owned exclusively by `hub-matches`
- No cross-schema foreign keys or database-level `JOIN` queries across schema boundaries are permitted.
- References to entities in other modules must be stored purely as raw `UUID` identifiers (e.g., `userId: UUID`, `matchId: UUID`).

#### RULE 3: Immutable DTOs via Java Records
- All Data Transfer Objects (DTOs), API request payloads, API response payloads, and cross-boundary event payloads must use Java `record` types.
- Regular classes with setters or mutable state must not be used as DTOs.

#### RULE 4: Mandatory OpenAPI v3 Documentation
- Every REST Controller must be documented with OpenAPI v3 (`springdoc-openapi`) annotations:
  - Controller class: `@Tag(name = "...", description = "...")`
  - Endpoints: `@Operation(summary = "...", description = "...")`
  - Documented responses: `@ApiResponse(responseCode = "...", description = "...")`
  - Request/Response fields documented where relevant using `@Schema`.

---

### 3. Verification & Compliance
- Every commit or pull request must pass `mvn clean compile` and automated architectural boundary validation.
- Violations of cross-module entity/repository imports or cross-schema references will fail automated review.

---

## 4. Git & Version Control Rules
- **Dedicated Feature Branches**: Every task must be developed on a dedicated feature branch (`feat/task-X.Y-short-name`) branched from `main`. Branches may only be merged into `main` after `mvn clean test` passes across the entire project.
- **Conventional Commits**: Commit messages must adhere to the Conventional Commits specification:
  - `feat(module): ...`
  - `fix(module): ...`
  - `test(module): ...`
  - `refactor(module): ...`
  - `docs(module): ...`
- **Frontend Markdown API Contracts**: Maintain up-to-date markdown API contracts inside `/contracts` (e.g. `/contracts/<module>-api.md`) for the Frontend Developer, containing endpoint summaries, parameter definitions, and realistic JSON request/response payloads.

---

## 5. Multi-Agent Orchestration Rules
1. **Lead Architect & Release Manager**: The Main Agent acts as Lead Architect & Release Manager, coordinating work, assigning tasks, and conducting final integration.
2. **Backend Subagent Scope**: Strictly confined to `d:/Return/CS` (`panda-cs`, Java 21 / Spring Boot, verified via `mvn test`).
3. **Frontend Subagent Scope**: Strictly confined to `d:/Return/CS-front/all-in-one-cs2-main` (`all-in-one-cs2`, React 19 / TypeScript / Vite 8, verified via `npx tsc --noEmit && npm run build`).
4. **Git Operation Restrictions**: Subagents must **NEVER** run `git commit`, `git merge`, or `git push`. Only the Main Agent executes Git operations after verifying all subagent test gates pass.
