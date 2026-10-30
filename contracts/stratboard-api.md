# CS2 SuperHub — Stratboard & Collaborative Whiteboard API Contract

**Module:** `hub-stratboard`  
**Base URL:** `http://localhost:8080`  
**WebSocket STOMP URL:** `ws://localhost:8080/ws/stratboard` (with SockJS fallback at `http://localhost:8080/ws/stratboard`)  
**Specification Version:** `v0.4.0`

---

## 1. Overview & Architectural Design

The Stratboard module provides three major capabilities for CS2 competitive teams and community players:
1. **Curated Utility Lineups Library:** Smokes, flashes, molotovs, and HE grenades with normalized radar coordinates (`0.0`–`100.0`), throw techniques (`JUMP_THROW`, `RUN_THROW`, etc.), and demonstration video URLs.
2. **Saved Tactics & Playbooks:** Permanent tactical playbooks persisted in PostgreSQL with shareable 8-character codes (`shareCode`) and author profile resolution via `UserLookupService` SPI.
3. **Real-Time Collaborative Tactical Rooms (STOMP WebSocket):** Low-latency whiteboard synchronization across teammates (freehand drawing, utility pins, player tokens, arrow vectors, real-time cursor positions) with zero-login friction (guests can join and draw immediately; only persisting as a permanent playbook requires a Steam account).

---

## 2. Utility Lineups REST API

Base path: `/api/v1/stratboard/lineups`

### 2.1. Browse Grenade Lineups Catalog
- **Method:** `GET /api/v1/stratboard/lineups`
- **Auth:** Public
- **Query Parameters:**
  - `mapName` (optional, string): e.g. `de_mirage`, `de_inferno`, `de_dust2`, `de_nuke`, `de_ancient`, `de_anubis`, `de_train`, `de_vertigo`
  - `utilityType` (optional, enum): `SMOKE`, `FLASH`, `MOLOTOV`, `HE`
  - `teamSide` (optional, enum): `T`, `CT`
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "7b3a4a15-0d32-4e89-a548-2fe4017fbb30",
      "mapName": "de_mirage",
      "utilityType": "SMOKE",
      "teamSide": "T",
      "title": "Mirage Window Smoke from T Spawn",
      "throwType": "JUMP_THROW",
      "standX": 34.0,
      "standY": 78.0,
      "landX": 48.0,
      "landY": 46.0,
      "videoUrl": "https://youtube.com/watch?v=mirage_window",
      "description": "Align with trash can near T Spawn, crosshair on top-right antenna corner, D+Jumpthrow",
      "createdByUserId": null,
      "isVerified": true,
      "createdAt": "2026-10-30T10:00:00Z"
    }
  ],
  "timestamp": "2026-10-30T14:00:00Z"
}
```

### 2.2. Get Lineup by ID
- **Method:** `GET /api/v1/stratboard/lineups/{lineupId}`
- **Auth:** Public
- **Response `200 OK`:** Full `UtilityLineupDto`
- **Response `404 Not Found`:** Lineup does not exist

### 2.3. Submit Community Lineup
- **Method:** `POST /api/v1/stratboard/lineups`
- **Auth:** Required (`Bearer <JWT>`)
- **Request Body:**
```json
{
  "mapName": "de_ancient",
  "utilityType": "SMOKE",
  "teamSide": "T",
  "title": "Cave Smoke from Red",
  "throwType": "STANDING",
  "standX": 25.4,
  "standY": 65.2,
  "landX": 38.0,
  "landY": 48.5,
  "videoUrl": "https://youtu.be/example",
  "description": "Aim at the highest leaf on the palm tree"
}
```
- **Response `201 Created`:** Created `UtilityLineupDto` with `isVerified: false`.

### 2.4. Delete Lineup
- **Method:** `DELETE /api/v1/stratboard/lineups/{lineupId}`
- **Auth:** Required (Author or `ADMIN`)
- **Response `200 OK`**

---

## 3. Saved Tactics & Playbooks REST API

Base path: `/api/v1/stratboard/tactics`

### 3.1. Browse Public Community Tactics
- **Method:** `GET /api/v1/stratboard/tactics`
- **Auth:** Public
- **Query Parameters:**
  - `mapName` (optional): Filter by CS2 map
  - `teamSide` (optional): `T` or `CT`
  - `page` (optional, default `0`): Page index
  - `size` (optional, default `20`): Page size
- **Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "d1354bb4-30d7-4632-9cb7-7a52f9b17726",
        "authorUserId": "480514df-2770-4496-aa4b-1df6c740b18e",
        "title": "Mirage Fast A Execute with 3 Palace",
        "mapName": "de_mirage",
        "teamSide": "T",
        "description": "Triple smoke execute with palace flashbang pop",
        "isPublic": true,
        "upvotes": 42,
        "shareCode": "FAST39AK",
        "author": {
          "userId": "480514df-2770-4496-aa4b-1df6c740b18e",
          "steamId": "76561198000000001",
          "username": "IGL_Aleksi",
          "avatarUrl": "https://avatars.steamstatic.com/sample.jpg"
        },
        "createdAt": "2026-10-30T12:00:00Z",
        "updatedAt": "2026-10-30T12:30:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "timestamp": "2026-10-30T14:00:00Z"
}
```

### 3.2. Get User's Saved Tactics
- **Method:** `GET /api/v1/stratboard/tactics/my`
- **Auth:** Required (`Bearer <JWT>`)
- **Response `200 OK`:** List of `TacticSummaryDto` (includes both public and private tactics authored by user)

### 3.3. Get Tactic Detail by UUID or 8-Char Share Code
- **Method:** `GET /api/v1/stratboard/tactics/{idOrShareCode}`
- **Auth:** Public (for public tactics); Authenticated author required for private tactics
- **Example URIs:**
  - `GET /api/v1/stratboard/tactics/FAST39AK` (via share code)
  - `GET /api/v1/stratboard/tactics/d1354bb4-30d7-4632-9cb7-7a52f9b17726` (via UUID)
- **Response `200 OK`:** Returns `TacticDetailDto` with full `canvasStateJson`.

### 3.4. Create & Save Tactic
- **Method:** `POST /api/v1/stratboard/tactics`
- **Auth:** Required (`Bearer <JWT>`)
- **Request Body:**
```json
{
  "title": "Inferno B Split with Arch Smoke",
  "mapName": "de_inferno",
  "teamSide": "T",
  "description": "Fast banana control then split CT and banana",
  "canvasStateJson": "{\"version\":1,\"elements\":[{\"elementId\":\"elem-1\",\"elementType\":\"ARROW\",\"x\":30.0,\"y\":45.0,\"endX\":50.0,\"endY\":60.0,\"colorHex\":\"#EF4444\"}]}",
  "isPublic": true
}
```
- **Response `201 Created`:** Returns `TacticDetailDto` with auto-generated 8-character `shareCode`.

### 3.5. Update Tactic
- **Method:** `PUT /api/v1/stratboard/tactics/{tacticId}`
- **Auth:** Required (Author or `ADMIN`)
- **Response `200 OK`:** Updated `TacticDetailDto`

### 3.6. Delete Tactic
- **Method:** `DELETE /api/v1/stratboard/tactics/{tacticId}`
- **Auth:** Required (Author or `ADMIN`)
- **Response `200 OK`**

---

## 4. Collaborative Live Rooms REST API

Base path: `/api/v1/stratboard/rooms`

### 4.1. Create a Collaborative Room
- **Method:** `POST /api/v1/stratboard/rooms`
- **Auth:** Public (optional JWT context attached if user is logged in)
- **Request Body:**
```json
{
  "mapName": "de_mirage",
  "teamSide": "T",
  "hostDisplayName": "s1mple",
  "hostColorHex": "#3B82F6",
  "initialTacticIdOrShareCode": "FAST39AK"
}
```
- **Response `201 Created`:**
```json
{
  "success": true,
  "message": "Collaborative room created successfully",
  "data": {
    "roomCode": "X7K9M2P4",
    "mapName": "de_mirage",
    "teamSide": "T",
    "hostUserId": "480514df-2770-4496-aa4b-1df6c740b18e",
    "hostParticipantId": "host-8af8a5bf",
    "participants": [
      {
        "participantId": "host-8af8a5bf",
        "displayName": "s1mple",
        "colorHex": "#3B82F6",
        "userId": "480514df-2770-4496-aa4b-1df6c740b18e",
        "isHost": true
      }
    ],
    "canvasElements": [],
    "createdAt": "2026-10-30T14:10:00Z",
    "lastActivityAt": "2026-10-30T14:10:00Z"
  },
  "timestamp": "2026-10-30T14:10:00Z"
}
```

### 4.2. Get Live Room Snapshot
- **Method:** `GET /api/v1/stratboard/rooms/{roomCode}`
- **Auth:** Public
- **Description:** Called when any teammate opens the invite link `https://cs2hub.gg/stratboard/X7K9M2P4`. Immediately renders all existing drawings, tokens, and active participants.
- **Response `200 OK`:** Full `StratRoomSnapshotDto`

### 4.3. Load Saved Tactic into Active Room
- **Method:** `POST /api/v1/stratboard/rooms/{roomCode}/load-tactic/{idOrShareCode}`
- **Auth:** Public (optional JWT)
- **Description:** Replaces current board state with a saved tactic from library and broadcasts `LOAD_TACTIC` event to all connected WebSocket subscribers.
- **Response `200 OK`:** Updated `StratRoomSnapshotDto`

### 4.4. Save Room as Persistent Tactic Playbook
- **Method:** `POST /api/v1/stratboard/rooms/{roomCode}/save-tactic`
- **Auth:** Required (`Bearer <JWT>`)
- **Request Body:**
```json
{
  "title": "Match vs Faze Clan - Mirage A Play",
  "description": "Round 14 pistol buy execute",
  "isPublic": false
}
```
- **Response `201 Created`:** Returns `TacticDetailDto` with new `shareCode` and permanent PostgreSQL storage.

---

## 5. Real-Time WebSocket (STOMP) Specification

The frontend connects to the Spring WebSocket Message Broker using the STOMP protocol over standard WebSockets or SockJS.

### 5.1. Connection Details
- **STOMP Endpoint:** `/ws/stratboard` (or with SockJS: `http://localhost:8080/ws/stratboard`)
- **Subscription Destination:** `/topic/stratboard/rooms/{roomCode}`
- **Publish Destination:** `/app/stratboard/rooms/{roomCode}/event`

### 5.2. Frontend Client Setup Example (STOMP.js)
```typescript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stratboard',
  onConnect: () => {
    client.subscribe('/topic/stratboard/rooms/X7K9M2P4', (message) => {
      const event: StratBoardWsBroadcast = JSON.parse(message.body);
      handleIncomingEvent(event);
    });

    // Notify room of join
    client.publish({
      destination: '/app/stratboard/rooms/X7K9M2P4/event',
      body: JSON.stringify({
        eventType: 'PARTICIPANT_JOIN',
        participantId: 'my-part-id',
        participant: {
          participantId: 'my-part-id',
          displayName: 'b1t',
          colorHex: '#10B981',
          userId: null,
          isHost: false
        }
      })
    });
  }
});

client.activate();
```

### 5.3. WebSocket Event Payloads (Inbound & Outbound)

#### A. `PARTICIPANT_JOIN`
```json
{
  "eventType": "PARTICIPANT_JOIN",
  "participantId": "part-2345",
  "participant": {
    "participantId": "part-2345",
    "displayName": "ZywOo",
    "colorHex": "#F59E0B",
    "userId": null,
    "isHost": false
  }
}
```

#### B. `PARTICIPANT_LEAVE`
```json
{
  "eventType": "PARTICIPANT_LEAVE",
  "participantId": "part-2345"
}
```

#### C. `CURSOR_MOVE` (High Frequency, Ephemeral)
```json
{
  "eventType": "CURSOR_MOVE",
  "participantId": "part-2345",
  "cursor": {
    "x": 52.4,
    "y": 38.1
  }
}
```

#### D. `ELEMENT_UPSERT` (Drawings, Arrows, Tokens, Utility Pins)
```json
{
  "eventType": "ELEMENT_UPSERT",
  "participantId": "part-host",
  "element": {
    "elementId": "elem-smoke-window",
    "elementType": "UTILITY_PIN",
    "x": 48.0,
    "y": 46.0,
    "colorHex": "#9CA3AF",
    "strokeWidth": 2.0,
    "text": "Window Smoke",
    "label": "SMOKE",
    "lineupId": "7b3a4a15-0d32-4e89-a548-2fe4017fbb30",
    "createdByParticipantId": "part-host"
  }
}
```

#### E. `ELEMENT_DELETE`
```json
{
  "eventType": "ELEMENT_DELETE",
  "participantId": "part-host",
  "elementId": "elem-smoke-window"
}
```

#### F. `BOARD_CLEAR`
```json
{
  "eventType": "BOARD_CLEAR",
  "participantId": "part-host"
}
```

#### G. `MAP_CHANGE`
```json
{
  "eventType": "MAP_CHANGE",
  "participantId": "part-host",
  "mapName": "de_inferno",
  "teamSide": "CT"
}
```

#### H. `LOAD_TACTIC`
```json
{
  "eventType": "LOAD_TACTIC",
  "participantId": "part-host",
  "tacticIdOrShareCode": "FAST39AK"
}
```

---

## 6. Error Responses
All REST endpoints adhere to the standard `ApiResponseDto<T>` or `ErrorResponseDto`:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Stratboard room not found with code: INVALID99",
  "timestamp": "2026-10-30T14:15:00Z",
  "path": "/api/v1/stratboard/rooms/INVALID99"
}
```
