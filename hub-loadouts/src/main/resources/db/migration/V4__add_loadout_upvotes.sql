-- Flyway Migration: V4__add_loadout_upvotes.sql
-- Module: hub-loadouts
-- Purpose: Deduplicate user upvotes on community loadouts

CREATE TABLE IF NOT EXISTS loadouts.loadout_upvotes (
    id UUID PRIMARY KEY,
    loadout_id UUID NOT NULL REFERENCES loadouts.user_loadouts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_loadout_upvotes_loadout_user UNIQUE (loadout_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_loadout_upvotes_loadout_id ON loadouts.loadout_upvotes(loadout_id);
CREATE INDEX IF NOT EXISTS idx_loadout_upvotes_user_id ON loadouts.loadout_upvotes(user_id);
