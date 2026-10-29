-- Initialize Identity Schema and Users Table
CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE IF NOT EXISTS identity.users (
    id UUID PRIMARY KEY,
    steam_id VARCHAR(32) NOT NULL UNIQUE,
    username VARCHAR(128) NOT NULL,
    avatar_url TEXT,
    profile_url TEXT,
    trade_url TEXT,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_steam_id ON identity.users(steam_id);
