-- Flyway Migration: V10__enrich_csnades_schema.sql
-- Module: hub-stratboard
-- Purpose: Add csnades.gg detailed schema fields (callouts, movement, technique, setpos, isPro, views)

ALTER TABLE stratboard.utility_lineups
    ADD COLUMN IF NOT EXISTS from_callout VARCHAR(64),
    ADD COLUMN IF NOT EXISTS to_callout VARCHAR(64),
    ADD COLUMN IF NOT EXISTS movement VARCHAR(32) DEFAULT 'Stationary',
    ADD COLUMN IF NOT EXISTS technique VARCHAR(32) DEFAULT 'Left Click',
    ADD COLUMN IF NOT EXISTS precision VARCHAR(32) DEFAULT 'Precise',
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(32) DEFAULT 'Medium',
    ADD COLUMN IF NOT EXISTS is_pro BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS views_count INT DEFAULT 1200,
    ADD COLUMN IF NOT EXISTS setpos_command VARCHAR(256),
    ADD COLUMN IF NOT EXISTS air_time_seconds DOUBLE PRECISION DEFAULT 4.5;

CREATE INDEX IF NOT EXISTS idx_lineups_to_callout ON stratboard.utility_lineups(map_name, to_callout);
