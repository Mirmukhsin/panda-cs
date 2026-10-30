# Minigames & "Guess the Elo" API Contract

This document provides API specifications, anti-cheat mechanisms, JSON payloads, and integration instructions for the Frontend Developer building the "Guess the Elo" game UI and community clip submission forms.

---

## 1. Overview & Architecture

The "Guess the Elo" minigame runs as a daily browser-based challenge (1 challenge per calendar date at UTC midnight):
- Each daily challenge consists of **3 distinct CS2 gameplay clips** (Rounds 1, 2, and 3).
- Players estimate the CS2 Premier Rating of the player in each clip (range: `1000` to `35000`).
- **Zero-Friction Guest Play:** Unauthenticated users can play instantly using the `X-Guest-Session-Id` HTTP header. Authenticated users (via Bearer JWT) are eligible for the Daily and All-Time Leaderboards.
- **Anti-Cheat Enforcement:** The backend guarantees that `actualPremierRating` and `actualFaceitLevel` are strictly `null` in the round clips response until the user or guest submits a guess for that specific clip.

---

## 2. Scoring & Accuracy Tiers

Points awarded per round are based on `diff = Math.abs(actualPremierRating - guessedPremierRating)`:

| Difference (`diff`) | Points Range | Tier Name | Emoji | Description |
| :--- | :--- | :--- | :---: | :--- |
| **0 – 500** | **1,000 pts** | `BULLSEYE` | 🟢 | Spot-on estimation |
| **501 – 2,000** | **999 – 500 pts** | `CLOSE` | 🟡 | Linear interpolation |
| **2,001 – 5,000** | **499 – 100 pts** | `GRAZED` | 🟠 | Linear interpolation |
| **> 5,000** | **0 pts** | `MISS` | 🔴 | Completely off mark |

Maximum score per day: **3,000 points** (3 rounds × 1,000 max).

When all 3 rounds are completed, the backend generates a Wordle-style Discord share summary:
```text
🎯 CS2Hub Guess the Elo (2026-10-30)
Round 1: 🟢 | Round 2: 🟡 | Round 3: 🔴
Total Score: 1750 / 3000
```

---

## 3. Endpoints

### 3.1 Get Today's Daily Challenge
- **URL:** `GET /api/v1/minigames/daily`
- **Security:** Public (Optional Bearer JWT or `X-Guest-Session-Id`)
- **Headers (Optional):**
  - `Authorization: Bearer <jwt_token>` (for authenticated players)
  - `X-Guest-Session-Id: <uuid_or_string>` (for anonymous guests; e.g. stored in `localStorage`)
- **Response (`200 OK` - Before Guessing):**
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "challengeId": "a1b2c3d4-0000-0000-0000-000000000001",
      "challengeDate": "2026-10-30",
      "rounds": [
        {
          "roundOrder": 1,
          "clipId": "c1f7b752-6b94-4d89-9831-50e599b50b55",
          "title": "Mirage A Site 1v3 Clutch",
          "videoUrl": "https://www.youtube.com/watch?v=sample1",
          "videoProvider": "YOUTUBE",
          "mapName": "de_mirage",
          "actualPremierRating": null,
          "actualFaceitLevel": null,
          "userGuess": null
        },
        {
          "roundOrder": 2,
          "clipId": "c2f7b752-6b94-4d89-9831-50e599b50b56",
          "title": "Inferno Banana Anti-Eco Ace",
          "videoUrl": "https://www.youtube.com/watch?v=sample2",
          "videoProvider": "YOUTUBE",
          "mapName": "de_inferno",
          "actualPremierRating": null,
          "actualFaceitLevel": null,
          "userGuess": null
        },
        {
          "roundOrder": 3,
          "clipId": "c3f7b752-6b94-4d89-9831-50e599b50b57",
          "title": "Dust2 Mid To B Spray Transfer",
          "videoUrl": "https://www.youtube.com/watch?v=sample3",
          "videoProvider": "YOUTUBE",
          "mapName": "de_dust2",
          "actualPremierRating": null,
          "actualFaceitLevel": null,
          "userGuess": null
        }
      ],
      "totalScore": 0,
      "completed": false,
      "shareText": null
    },
    "timestamp": "2026-10-30T10:00:00Z"
  }
  ```

---

### 3.2 Submit a Guess
- **URL:** `POST /api/v1/minigames/daily/guess`
- **Security:** Public (Requires Bearer JWT OR `X-Guest-Session-Id` header)
- **Request Body:**
  ```json
  {
    "clipId": "c1f7b752-6b94-4d89-9831-50e599b50b55",
    "guessedPremierRating": 16000
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Guess submitted and scored",
    "data": {
      "clipId": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "roundOrder": 1,
      "actualPremierRating": 16450,
      "actualFaceitLevel": 7,
      "guessedPremierRating": 16000,
      "difference": 450,
      "pointsAwarded": 1000,
      "tierEmoji": "🟢",
      "totalChallengeScore": 1000,
      "completed": false,
      "shareText": null
    },
    "timestamp": "2026-10-30T10:01:00Z"
  }
  ```
- **Error Response (`400 Bad Request` - Duplicate Guess):**
  ```json
  {
    "status": 400,
    "error": "BAD_REQUEST",
    "message": "A guess has already been submitted for this clip in today's challenge",
    "details": [],
    "timestamp": "2026-10-30T10:01:30Z"
  }
  ```

---

### 3.3 Get Leaderboard
- **URL:** `GET /api/v1/minigames/leaderboard`
- **Security:** Public
- **Query Parameters:**
  - `period`: `DAILY` (default) or `ALL_TIME`
  - `date`: `YYYY-MM-DD` (optional, defaults to today UTC for `DAILY`)
  - `limit`: `20` (default, up to 100)
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "period": "DAILY",
      "date": "2026-10-30",
      "entries": [
        {
          "rank": 1,
          "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "username": "s1mple",
          "avatarUrl": "https://avatars.steamstatic.com/example_s1mple.jpg",
          "score": 2850,
          "roundsPlayed": 3
        },
        {
          "rank": 2,
          "userId": "b2c3d4e5-f6a7-8901-bcde-fa2345678901",
          "username": "ZywOo",
          "avatarUrl": "https://avatars.steamstatic.com/example_zywoo.jpg",
          "score": 2600,
          "roundsPlayed": 3
        }
      ]
    },
    "timestamp": "2026-10-30T10:05:00Z"
  }
  ```

---

### 3.4 Community Clip Submission (Player)
- **URL:** `POST /api/v1/minigames/clips`
- **Security:** Requires `Authorization: Bearer <jwt_token>`
- **Request Body:**
  ```json
  {
    "title": "Clean Mirage A execute 1v3 clutch",
    "videoUrl": "https://www.youtube.com/watch?v=sample123",
    "mapName": "de_mirage",
    "actualPremierRating": 17500,
    "actualFaceitLevel": 8
  }
  ```
- **Response (`201 Created`):** Returns `ClipSubmissionResponseDto` with status `PENDING`.

### 3.5 My Submitted Clips
- **URL:** `GET /api/v1/minigames/clips/my`
- **Security:** Requires `Authorization: Bearer <jwt_token>`
- **Response (`200 OK`):** Returns array of user's submitted clips with statuses (`PENDING`, `APPROVED`, `REJECTED`, `USED`).

---

### 3.6 Admin Moderation & Challenge Generation
- **List Clips for Review:** `GET /api/v1/minigames/admin/clips?status=PENDING` (Requires `ADMIN` role)
- **Review Clip:** `PATCH /api/v1/minigames/admin/clips/{clipId}/review` (Requires `ADMIN` role)
  ```json
  {
    "status": "APPROVED",
    "overridePremierRating": 18000
  }
  ```
- **Manually Generate Daily Challenge:** `POST /api/v1/minigames/admin/daily/generate?seedIfEmpty=true` (Requires `ADMIN` role)
  - Instantly generates today's challenge so local frontend developers do not have to wait for the midnight cron job or manually submit and approve 3 clips.
