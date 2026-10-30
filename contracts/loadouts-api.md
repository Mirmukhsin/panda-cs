# CS2Hub - Loadouts Module API Contract (`hub-loadouts`)

This contract defines all endpoints available in the `hub-loadouts` module, covering:
1. **Catalog Search & Filter APIs** (Skins, Wear Grades, and Stickers)
2. **Color-Matched Budget Loadout Generator** (Algorithmic, zero-login friction)
3. **Community Loadouts & Sticker Crafts** (Publishing, Upvoting, Personal Feed)
4. **Admin Catalog Management** (Curated Seeding, External Sync)

Base URL: `/api/v1/loadouts`

---

## 1. Catalog APIs (`CatalogController`)

### `GET /api/v1/loadouts/catalog/skins`
Returns a paginated list of CS2 weapon skins filtered by color theme, weapon, category, team side, max price, or search text.

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|---|---|---|---|---|
| `color` | String (enum) | No | `RED`, `BLUE`, `PURPLE`, `GREEN`, `YELLOW`, `ORANGE`, `WHITE`, `BLACK`, `PINK`, `GOLD` | `RED` |
| `weaponId` | String | No | Technical weapon ID | `weapon_ak47` |
| `category` | String (enum) | No | `PISTOLS`, `RIFLES`, `SMGS`, `HEAVY`, `KNIVES`, `GLOVES` | `RIFLES` |
| `teamSide` | String (enum) | No | `T`, `CT`, `BOTH` | `T` |
| `maxPrice` | Number | No | Budget ceiling for lowest available wear price | `50.00` |
| `q` | String | No | Search query in skin display name | `asiimov` |
| `page` | Integer | No | Page index (default: `0`) | `0` |
| `size` | Integer | No | Page size (default: `20`, max: `100`) | `20` |
| `sort` | String | No | Field and direction (default: `lowestPriceUsd,asc`) | `lowestPriceUsd,asc` |

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "skin-ak47-redline",
        "name": "AK-47 | Redline",
        "weaponId": "weapon_ak47",
        "weaponName": "AK-47",
        "category": "RIFLES",
        "teamSide": "T",
        "rarity": "Classified",
        "primaryColor": "RED",
        "secondaryColor": "BLACK",
        "imageUrl": "https://community.cloudflare.steamstatic.com/economy/image/...",
        "minFloat": 0.10,
        "maxFloat": 0.70,
        "priceFn": null,
        "priceMw": 35.00,
        "priceFt": 18.50,
        "priceWw": 16.00,
        "priceBs": 14.00,
        "lowestPriceUsd": 14.00
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

### `GET /api/v1/loadouts/catalog/skins/{skinId}`
Returns detailed metadata and wear pricing for a single weapon skin.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "skin-ak47-asiimov",
    "name": "AK-47 | Asiimov",
    "weaponId": "weapon_ak47",
    "weaponName": "AK-47",
    "category": "RIFLES",
    "teamSide": "T",
    "rarity": "Covert",
    "primaryColor": "ORANGE",
    "secondaryColor": "WHITE",
    "imageUrl": "https://community.cloudflare.steamstatic.com/economy/image/...",
    "minFloat": 0.05,
    "maxFloat": 0.70,
    "priceFn": 240.00,
    "priceMw": 85.00,
    "priceFt": 45.00,
    "priceWw": 35.00,
    "priceBs": 28.00,
    "lowestPriceUsd": 28.00
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

### `GET /api/v1/loadouts/catalog/stickers`
Returns a paginated list of CS2 tournament and community stickers.

**Query Parameters:** `color`, `maxPrice`, `q`, `page`, `size`, `sort` (default: `priceUsd,asc`).

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "sticker-titan-holo-katowice-2014",
        "name": "Sticker | Titan (Holo) | Katowice 2014",
        "rarity": "Exotic",
        "primaryColor": "BLUE",
        "imageUrl": "https://community.cloudflare.steamstatic.com/economy/image/...",
        "priceUsd": 55000.00
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

## 2. Color-Matched Budget Generator (`LoadoutGeneratorController`)

### `POST /api/v1/loadouts/generate`
Public endpoint (zero login required) that greedily selects matching skins across core weapon slots, optimizes wear conditions to fit under the target budget, and maximizes value.

**Request Body:**
```json
{
  "budgetUsd": 50.00,
  "themeColor": "RED",
  "preferredCtRifle": "weapon_m4a1_silencer",
  "includeKnifeAndGloves": false
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Color-matched budget loadout generated successfully",
  "data": {
    "themeColor": "RED",
    "budgetLimitUsd": 50.00,
    "totalCostUsd": 47.90,
    "remainingBudgetUsd": 2.10,
    "items": [
      {
        "slot": "T_RIFLE",
        "weaponId": "weapon_ak47",
        "weaponName": "AK-47",
        "skinId": "skin-ak47-redline",
        "skinName": "AK-47 | Redline",
        "wearCondition": "FT",
        "priceUsd": 18.50,
        "primaryColor": "RED",
        "imageUrl": "https://..."
      },
      {
        "slot": "CT_RIFLE",
        "weaponId": "weapon_m4a1_silencer",
        "weaponName": "M4A1-S",
        "skinId": "skin-m4a1s-cyrex",
        "skinName": "M4A1-S | Cyrex",
        "wearCondition": "FT",
        "priceUsd": 20.00,
        "primaryColor": "RED",
        "imageUrl": "https://..."
      },
      {
        "slot": "AWP",
        "weaponId": "weapon_awp",
        "weaponName": "AWP",
        "skinId": "skin-awp-atheris",
        "skinName": "AWP | Atheris",
        "wearCondition": "BS",
        "priceUsd": 3.20,
        "primaryColor": "GREEN",
        "imageUrl": "https://..."
      },
      {
        "slot": "T_PISTOL",
        "weaponId": "weapon_glock",
        "weaponName": "Glock-18",
        "skinId": "skin-glock-water-elemental",
        "skinName": "Glock-18 | Water Elemental",
        "wearCondition": "BS",
        "priceUsd": 6.50,
        "primaryColor": "RED",
        "imageUrl": "https://..."
      }
    ]
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

## 3. Community Loadouts & Sticker Crafts (`CommunityLoadoutController`)

### `GET /api/v1/loadouts/community`
Browse public community loadouts feed.

**Query Parameters:**
- `color`: (optional) Theme color filter (`RED`, `BLUE`, etc.)
- `maxBudget`: (optional) Maximum total cost
- `sortBy`: `popular` (default, sorted by upvotes) or `latest` (sorted by creation date)
- `page`: default `0`
- `size`: default `20`

**Headers (Optional):**
- `Authorization: Bearer <JWT>`: When supplied, each loadout indicates `upvotedByCurrentUser: true/false`.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "c1f7a0b2-3c4d-5e6f-7a8b-9c0d1e2f3a4b",
        "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "title": "Crimson Blood - Full Red Loadout",
        "themeColor": "RED",
        "budgetLimitUsd": 100.00,
        "totalCostUsd": 92.50,
        "upvotes": 42,
        "isPublic": true,
        "itemsCount": 8,
        "author": {
          "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "steamId": "76561198000000001",
          "username": "broky",
          "avatarUrl": "https://avatars.steamstatic.com/..."
        },
        "upvotedByCurrentUser": false,
        "createdAt": "2026-10-30T16:00:00Z",
        "updatedAt": "2026-10-30T16:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

### `POST /api/v1/loadouts/community`
**Requires Authentication (`Bearer <JWT>`)**

Saves a new weapon loadout and custom sticker crafts.

**Request Body:**
```json
{
  "title": "Titan Blue Dream",
  "themeColor": "BLUE",
  "budgetLimitUsd": 120.00,
  "isPublic": true,
  "items": [
    {
      "weaponSlot": "T_RIFLE",
      "skinId": "skin-ak47-vulcan",
      "wearCondition": "FT",
      "stickers": [
        {
          "stickerId": "sticker-cloud9-g2a-holo-katowice-2015",
          "slotIndex": 0,
          "offsetX": 0.0,
          "offsetY": 0.0,
          "rotation": 0.0,
          "scale": 1.0
        }
      ]
    }
  ]
}
```

**Response `201 Created`:** Returns `UserLoadoutDetailDto`.

---

### `GET /api/v1/loadouts/community/{loadoutId}`
Retrieves full loadout details including weapon slots, wear prices, attached sticker craft coordinates, and creator info.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "c1f7a0b2-3c4d-5e6f-7a8b-9c0d1e2f3a4b",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "title": "Titan Blue Dream",
    "themeColor": "BLUE",
    "budgetLimitUsd": 120.00,
    "totalCostUsd": 1180.00,
    "upvotes": 5,
    "isPublic": true,
    "items": [
      {
        "id": "e8d2a1b0-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
        "weaponSlot": "T_RIFLE",
        "skinId": "skin-ak47-vulcan",
        "skinName": "AK-47 | Vulcan",
        "weaponName": "AK-47",
        "wearCondition": "FT",
        "selectedPriceUsd": 1230.00,
        "primaryColor": "BLUE",
        "imageUrl": "https://...",
        "stickers": [
          {
            "stickerId": "sticker-cloud9-g2a-holo-katowice-2015",
            "slotIndex": 0,
            "offsetX": 0.0,
            "offsetY": 0.0,
            "rotation": 0.0,
            "scale": 1.0
          }
        ]
      }
    ],
    "author": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "steamId": "76561198000000001",
      "username": "broky",
      "avatarUrl": "https://avatars.steamstatic.com/..."
    },
    "upvotedByCurrentUser": true,
    "createdAt": "2026-10-30T16:00:00Z",
    "updatedAt": "2026-10-30T16:00:00Z"
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

### `POST /api/v1/loadouts/community/{loadoutId}/upvote`
**Requires Authentication (`Bearer <JWT>`)**

Toggles an upvote. If the user hasn't upvoted, adds an upvote. If they already upvoted, removes the upvote. Deduplicated per user.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Upvote updated",
  "data": {
    "loadoutId": "c1f7a0b2-3c4d-5e6f-7a8b-9c0d1e2f3a4b",
    "upvotes": 43,
    "upvoted": true
  },
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

### `DELETE /api/v1/loadouts/community/{loadoutId}`
**Requires Authentication (`Bearer <JWT>`)**

Deletes a loadout. Permitted only if the caller is the author or has `ADMIN` role.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Loadout deleted successfully",
  "data": null,
  "timestamp": "2026-10-30T17:30:00Z"
}
```

---

## 4. Admin APIs (`AdminCatalogController`)

- `POST /api/v1/loadouts/admin/seed`: Seeds database with 33 curated skins and 10 iconic tournament stickers.
- `POST /api/v1/loadouts/admin/sync`: Synchronizes latest skin and sticker metadata from `ByMykel/CSGO-API`.
- Requires `ADMIN` role.
