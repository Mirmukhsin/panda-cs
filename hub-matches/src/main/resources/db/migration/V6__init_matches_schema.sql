CREATE SCHEMA IF NOT EXISTS matches;

CREATE TABLE IF NOT EXISTS matches.match_analyses (
    id UUID PRIMARY KEY,
    share_code VARCHAR(128) NOT NULL UNIQUE,
    demo_url TEXT,
    submitted_by_user_id UUID,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    map_name VARCHAR(64),
    team_a_score INT DEFAULT 0,
    team_b_score INT DEFAULT 0,
    total_rounds INT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    error_message TEXT,
    share_slug VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS matches.match_players (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches.match_analyses(id) ON DELETE CASCADE,
    steam_id VARCHAR(32) NOT NULL,
    user_id UUID,
    player_name VARCHAR(128) NOT NULL,
    team_side VARCHAR(16) NOT NULL,
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    adr DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    headshot_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    kast_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    impact_rating DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    first_kills INT NOT NULL DEFAULT 0,
    first_deaths INT NOT NULL DEFAULT 0,
    clutches_won INT NOT NULL DEFAULT 0,
    team_flashes INT NOT NULL DEFAULT 0,
    enemies_flashed INT NOT NULL DEFAULT 0,
    utility_damage INT NOT NULL DEFAULT 0,
    zero_damage_rounds INT NOT NULL DEFAULT 0,
    eco_kills INT NOT NULL DEFAULT 0,
    awp_kills INT NOT NULL DEFAULT 0,
    bomb_plants INT NOT NULL DEFAULT 0,
    bomb_defuses INT NOT NULL DEFAULT 0,
    headline_title VARCHAR(128),
    headline_roast TEXT
);

CREATE TABLE IF NOT EXISTS matches.player_awards (
    id UUID PRIMARY KEY,
    match_player_id UUID NOT NULL REFERENCES matches.match_players(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES matches.match_analyses(id) ON DELETE CASCADE,
    badge_code VARCHAR(64) NOT NULL,
    badge_title VARCHAR(128) NOT NULL,
    badge_category VARCHAR(16) NOT NULL,
    badge_icon VARCHAR(32) NOT NULL,
    description TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_match_analyses_status_created ON matches.match_analyses(status, created_at);
CREATE INDEX IF NOT EXISTS idx_match_analyses_share_slug ON matches.match_analyses(share_slug);
CREATE INDEX IF NOT EXISTS idx_match_players_match_id ON matches.match_players(match_id);
CREATE INDEX IF NOT EXISTS idx_match_players_steam_id ON matches.match_players(steam_id);
CREATE INDEX IF NOT EXISTS idx_player_awards_match_player_id ON matches.player_awards(match_player_id);
CREATE INDEX IF NOT EXISTS idx_player_awards_match_id ON matches.player_awards(match_id);
