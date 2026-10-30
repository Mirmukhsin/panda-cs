-- Initialize Loadouts Schema and Core Catalog / Preset Tables
CREATE SCHEMA IF NOT EXISTS loadouts;

-- CS2 Weapon Skins Catalog
CREATE TABLE IF NOT EXISTS loadouts.skins (
    id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    weapon_id VARCHAR(64) NOT NULL,
    weapon_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    team_side VARCHAR(16) NOT NULL,
    rarity VARCHAR(64) NOT NULL,
    primary_color VARCHAR(32) NOT NULL,
    secondary_color VARCHAR(32),
    image_url TEXT NOT NULL,
    min_float DOUBLE PRECISION DEFAULT 0.0,
    max_float DOUBLE PRECISION DEFAULT 1.0,
    price_fn NUMERIC(10, 2),
    price_mw NUMERIC(10, 2),
    price_ft NUMERIC(10, 2),
    price_ww NUMERIC(10, 2),
    price_bs NUMERIC(10, 2),
    lowest_price_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- CS2 Stickers Catalog
CREATE TABLE IF NOT EXISTS loadouts.stickers (
    id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rarity VARCHAR(64),
    primary_color VARCHAR(32),
    image_url TEXT NOT NULL,
    price_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- User Custom Loadouts
CREATE TABLE IF NOT EXISTS loadouts.user_loadouts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    theme_color VARCHAR(32),
    budget_limit_usd NUMERIC(10, 2),
    total_cost_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    upvotes INT NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Items assigned to a Loadout
CREATE TABLE IF NOT EXISTS loadouts.user_loadout_items (
    id UUID PRIMARY KEY,
    loadout_id UUID NOT NULL REFERENCES loadouts.user_loadouts(id) ON DELETE CASCADE,
    skin_id VARCHAR(128) NOT NULL REFERENCES loadouts.skins(id),
    weapon_slot VARCHAR(64) NOT NULL,
    wear_condition VARCHAR(16) NOT NULL,
    selected_price_usd NUMERIC(10, 2) NOT NULL,
    stickers_json TEXT
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_skins_color_price ON loadouts.skins(primary_color, lowest_price_usd);
CREATE INDEX IF NOT EXISTS idx_skins_weapon ON loadouts.skins(weapon_id);
CREATE INDEX IF NOT EXISTS idx_skins_team ON loadouts.skins(team_side);
CREATE INDEX IF NOT EXISTS idx_user_loadouts_theme_upvotes ON loadouts.user_loadouts(theme_color, upvotes DESC);
CREATE INDEX IF NOT EXISTS idx_user_loadouts_user ON loadouts.user_loadouts(user_id);
