-- Flyway Migration: V5__init_stratboard_schema.sql
-- Module: hub-stratboard
-- Purpose: Initialize utility lineups and tactics tables

CREATE SCHEMA IF NOT EXISTS stratboard;

CREATE TABLE IF NOT EXISTS stratboard.utility_lineups (
    id UUID PRIMARY KEY,
    map_name VARCHAR(64) NOT NULL,
    utility_type VARCHAR(32) NOT NULL,
    team_side VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    throw_type VARCHAR(64) NOT NULL,
    stand_x DOUBLE PRECISION NOT NULL,
    stand_y DOUBLE PRECISION NOT NULL,
    land_x DOUBLE PRECISION NOT NULL,
    land_y DOUBLE PRECISION NOT NULL,
    video_url TEXT NOT NULL,
    description TEXT,
    created_by_user_id UUID,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stratboard.tactics (
    id UUID PRIMARY KEY,
    author_user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    map_name VARCHAR(64) NOT NULL,
    team_side VARCHAR(16) NOT NULL,
    description TEXT,
    canvas_state_json TEXT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    upvotes INT NOT NULL DEFAULT 0,
    share_code VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lineups_map_type_side ON stratboard.utility_lineups(map_name, utility_type, team_side);
CREATE INDEX IF NOT EXISTS idx_tactics_map_side_public ON stratboard.tactics(map_name, team_side, is_public, upvotes DESC);
CREATE INDEX IF NOT EXISTS idx_tactics_share_code ON stratboard.tactics(share_code);
CREATE INDEX IF NOT EXISTS idx_tactics_author ON stratboard.tactics(author_user_id);
