# Identity & Authentication API Contract

This document provides API specifications, JSON payloads, and testing instructions for the Frontend Developer integrating with `hub-identity`.

---

## 1. Standard Username & Password Authentication

Players can register and authenticate directly using their username, email, and password alongside Steam OpenID.

### A. Register Account
**Endpoint:** `POST /api/v1/auth/register`  
**Content-Type:** `application/json`

#### Request Payload
```json
{
  "username": "s1mple_pro",
  "email": "player@cs2hub.com",
  "password": "SecurePassword123!"
}
```

#### Response Payload (`201 Created`)
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 604800000,
    "user": {
      "id": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "steamId": null,
      "username": "s1mple_pro",
      "email": "player@cs2hub.com",
      "avatarUrl": "https://api.dicebear.com/7.x/bottts/svg?seed=s1mple_pro",
      "profileUrl": null,
      "tradeUrl": null,
      "role": "USER",
      "createdAt": "2026-11-01T14:30:00Z"
    }
  },
  "timestamp": "2026-11-01T14:30:00Z"
}
```

### B. Login with Password
**Endpoint:** `POST /api/v1/auth/login`  
**Content-Type:** `application/json`

#### Request Payload
```json
{
  "usernameOrEmail": "player@cs2hub.com",
  "password": "SecurePassword123!"
}
```
*(Accepts either the account username or email in `usernameOrEmail`).*

#### Response Payload (`200 OK`)
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 604800000,
    "user": {
      "id": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "steamId": null,
      "username": "s1mple_pro",
      "email": "player@cs2hub.com",
      "avatarUrl": "https://api.dicebear.com/7.x/bottts/svg?seed=s1mple_pro",
      "profileUrl": null,
      "tradeUrl": null,
      "role": "USER",
      "createdAt": "2026-11-01T14:30:00Z"
    }
  },
  "timestamp": "2026-11-01T14:30:00Z"
}
```

---

## 2. Local Development Authentication (Fast Test Token)

In local and development environments (`app.auth.dev-mode-enabled=true`), the backend provides a direct login endpoint that bypasses Steam OpenID and immediately issues a valid JWT access token.

### Quick Test JWT Generation

**Endpoint:** `POST /api/v1/auth/dev-login`  
**Content-Type:** `application/json`

#### Request Payload
```json
{
  "steamId": "76561198012345678",
  "username": "s1mple_dev",
  "role": "USER"
}
```
> **Note on Roles:** Pass `"role": "ADMIN"` if you need to test moderation or admin-only endpoints.

#### Response Payload (`200 OK`)
```json
{
  "success": true,
  "message": "Dev login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 604800000,
    "user": {
      "id": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "steamId": "76561198012345678",
      "username": "s1mple_dev",
      "avatarUrl": "https://avatars.steamstatic.com/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg",
      "profileUrl": "https://steamcommunity.com/profiles/76561198012345678",
      "tradeUrl": null,
      "role": "USER",
      "createdAt": "2026-10-30T00:30:00Z"
    }
  },
  "timestamp": "2026-10-30T00:30:00Z"
}
```

#### How to use the token in frontend requests:
Add the `Authorization` header to all authenticated requests:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 2. Production Steam OpenID 2.0 Flow

```
[ Frontend ]               [ Backend API ]               [ Steam Community ]
     |                             |                             |
     |-- 1. Click "Login with Steam" ->                         |
     |   GET /api/v1/auth/steam/login?redirect=false              |
     |<- 2. { loginUrl: "https://steamcommunity.com/..." } ------|
     |                                                           |
     |-- 3. Redirect browser to Steam Login -------------------->|
     |                                                           |
     |<-- 4. User logs in, Steam redirects browser to callback --|
     |       GET /api/v1/auth/steam/callback?openid.*...         |
     |                             |-- 5. Validates with Steam ->|
     |                             |<- 6. is_valid:true ---------|
     |<- 7. Redirects to frontend callback URL -------------------|
     |      http://localhost:3000/auth/callback?token=<JWT>       |
     |                                                           |
     |-- 8. Extract token from URL param and store in localStorage|
```

### 2.1 Initiate Steam Login
- **URL:** `GET /api/v1/auth/steam/login`
- **Query Params:**
  - `redirect=true` (default): Issues a 302 redirect directly to Steam OpenID.
  - `redirect=false`: Returns a JSON response containing the target login URL:
    ```json
    {
      "success": true,
      "message": "Steam login URL generated",
      "data": {
        "loginUrl": "https://steamcommunity.com/openid/login?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&..."
      },
      "timestamp": "2026-10-30T00:30:00Z"
    }
    ```

### 2.2 Steam Callback
- **URL:** `GET /api/v1/auth/steam/callback`
- **Query Params:** Forwarded query string from Steam OpenID verification.
- **Behavior:**
  - Default: Redirects browser to `${FRONTEND_AUTH_REDIRECT}?token=<accessToken>` (default: `http://localhost:3000/auth/callback?token=...`).
  - With `format=json`: Returns the `AuthTokenResponseDto` JSON payload directly.

---

## 3. User Profile Endpoints

### 3.1 Get Current User Profile
- **URL:** `GET /api/v1/users/me`
- **Headers:** `Authorization: Bearer <token>`
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "id": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "steamId": "76561198012345678",
      "username": "s1mple_dev",
      "avatarUrl": "https://avatars.steamstatic.com/example_full.jpg",
      "profileUrl": "https://steamcommunity.com/profiles/76561198012345678",
      "tradeUrl": "https://steamcommunity.com/tradeoffer/new/?partner=12345678&token=abcdefgh",
      "role": "USER",
      "createdAt": "2026-10-30T00:30:00Z"
    },
    "timestamp": "2026-10-30T00:30:00Z"
  }
  ```

### 3.2 Update Current User Profile
- **URL:** `PATCH /api/v1/users/me`
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:**
  ```json
  {
    "username": "s1mple_goat",
    "tradeUrl": "https://steamcommunity.com/tradeoffer/new/?partner=12345678&token=abcdefgh"
  }
  ```
  *(Both fields are optional)*
- **Response (`200 OK`):** Returns updated `UserProfileDto`.

### 3.3 Get Public User Summary
- **URL:** `GET /api/v1/users/{userId}`
- **Security:** Public (no Authorization header required)
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "userId": "c1f7b752-6b94-4d89-9831-50e599b50b55",
      "steamId": "76561198012345678",
      "username": "s1mple_goat",
      "avatarUrl": "https://avatars.steamstatic.com/example_full.jpg"
    },
    "timestamp": "2026-10-30T00:30:00Z"
  }
  ```
- **Error Response (`404 Not Found`):**
  ```json
  {
    "status": 404,
    "error": "RESOURCE_NOT_FOUND",
    "message": "User not found with ID: c1f7b752-6b94-4d89-9831-50e599b50b55",
    "details": [],
    "timestamp": "2026-10-30T00:30:00Z"
  }
  ```
