# CS2-SuperHub Architecture

## 1. Overview
**CS2-SuperHub** is a next-generation Counter-Strike 2 community platform built as a **Strict Modular Monolith** using **Java 21** and **Spring Boot 3.4+**.

It delivers a unified, high-performance platform for CS2 players, teams, and community members, integrating Steam OpenID 2.0 authentication, skin catalog sync & sticker craft showcase, daily browser-based minigames (e.g., "Guess the Elo"), real-time collaborative tactical stratboards, and automated match demo analysis.

---

## 2. High-Level Modular Monolith Design

```
                     +-----------------------+
                     |     hub-bootstrap     |  (Application Entrypoint & Assembly)
                     +-----------+-----------+
                                 |
        +------------+-----------+-----------+------------+------------+
        |            |                       |            |            |
        v            v                       v            v            v
  +------------+ +------------+       +------------+ +------------+ +------------+
  |hub-identity| |hub-loadouts|       |hub-minigames| |hub-stratboard| |hub-matches|
  +-----+------+ +-----+------+       +-----+------+ +-----+------+ +-----+------+
        |              |                    |              |              |
        +--------------+----------+---------+--------------+--------------+
                                  |
                                  v
                        +--------------------+
                        |     hub-common     |  (Shared utilities, records, base types)
                        +--------------------+
```

---

## 3. Module Boundaries & Responsibilities

| Module | Schema | Purpose & Responsibilities | Dependencies |
| :--- | :--- | :--- | :--- |
| **`hub-common`** | N/A | Base utilities, common DTO records, domain event interfaces, security filters/helpers, JWT authentication, global exception handling, cross-module SPI interfaces. | None (base module) |
| **`hub-identity`** | `identity` | Steam OpenID 2.0 authentication, JWT issuance, and user profile management. | `hub-common` |
| **`hub-loadouts`** | `loadouts` | CS2 skin & sticker catalog sync (`ByMykel/CSGO-API`), skin price tracking, Budget Loadout Generator, and community sticker crafts. | `hub-common` |
| **`hub-minigames`** | `minigames` | Browser-based daily CS2 minigames like "Guess the Elo" from video clips, daily cron challenges, and leaderboards (NOT CS2 game server hosting). | `hub-common` |
| **`hub-stratboard`** | `stratboard` | Real-time collaborative 2D map tactical canvas (Spring WebSockets/STOMP) and utility lineup playbooks. | `hub-common` |
| **`hub-matches`** | `matches` | CS2 `.dem` match demo analysis, "Roast & Flex" automated badge/award calculation, and shareable match summary cards. | `hub-common` |
| **`hub-bootstrap`** | Aggregated | `@SpringBootApplication` runner, active Spring profiles, Flyway migration coordinator, global application wiring. | All modules |

---

## 4. Architectural Rules & Constraints

### 4.1 Zero Cross-Module Entity / Repository Coupling
1. **Never import JPA `@Entity` or `@Repository` across modules.** Modules are decoupled domain units.
2. Sibling feature modules must never depend on each other at the Maven level (`pom.xml`).
3. If `hub-matches` needs identity details for a player, it must refer to `userId: UUID` and resolve player public info through asynchronous domain events or a lightweight client/facade interface returning record DTOs.

### 4.2 Database Schema Isolation
1. Dedicated PostgreSQL schemas for each module:
   - `identity`
   - `loadouts`
   - `minigames`
   - `stratboard`
   - `matches`
2. **No Foreign Keys across schemas.**
3. **No Cross-Schema SQL Joins.**
4. Foreign entities are referenced exclusively via immutable `UUID` scalar fields.
5. Each module defines its JPA entities with `@Table(schema = "<module_name>", name = "...")`.

### 4.3 DTO & API Standards
1. **Java Records for All DTOs:** Every API request, response, and event object must be declared as a Java `record` (immutable data carrier).
2. **OpenAPI v3 Standard:** Every controller must be explicitly annotated with:
   - `@Tag` at the class level
   - `@Operation` on each mapping method
   - `@ApiResponse` documenting HTTP status codes and payloads
   - Swagger UI is accessible at `/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`.

---

## 5. Technology Stack

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.4.x
- **Persistence:** Spring Data JPA, Hibernate 6
- **Database:** PostgreSQL 16 (isolated schemas)
- **Migrations:** Flyway Database Migrations
- **Security:** Spring Security (Stateless JWT / Steam OpenID)
- **API Documentation:** Springdoc OpenAPI v3 (`org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3`)
- **Code Generation:** Project Lombok
- **Testing:** JUnit 5, AssertJ, Mockito, Testcontainers
- **Containerization:** Docker Compose
