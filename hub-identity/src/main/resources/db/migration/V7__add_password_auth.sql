-- Migration V7: Add username/password authentication support to identity.users

ALTER TABLE identity.users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

ALTER TABLE identity.users
    ALTER COLUMN steam_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON identity.users(email) WHERE email IS NOT NULL;
