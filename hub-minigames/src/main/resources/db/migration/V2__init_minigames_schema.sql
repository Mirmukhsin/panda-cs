-- Initialize Minigames Schema and Core Tables for Daily "Guess the Elo"
CREATE SCHEMA IF NOT EXISTS minigames;

-- Clip Submissions & Pool
CREATE TABLE IF NOT EXISTS minigames.clips (
    id UUID PRIMARY KEY,
    submitted_by_user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    video_url TEXT NOT NULL,
    video_provider VARCHAR(32) NOT NULL,
    map_name VARCHAR(64) NOT NULL,
    actual_premier_rating INT NOT NULL,
    actual_faceit_level INT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Daily Challenges (1 per calendar date)
CREATE TABLE IF NOT EXISTS minigames.daily_challenges (
    id UUID PRIMARY KEY,
    challenge_date DATE NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Clips assigned to a Daily Challenge round
CREATE TABLE IF NOT EXISTS minigames.daily_challenge_clips (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL REFERENCES minigames.daily_challenges(id) ON DELETE CASCADE,
    clip_id UUID NOT NULL REFERENCES minigames.clips(id),
    round_order INT NOT NULL,
    CONSTRAINT uq_challenge_round UNIQUE (challenge_id, round_order)
);

-- User Guess Submissions
CREATE TABLE IF NOT EXISTS minigames.user_guesses (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL REFERENCES minigames.daily_challenges(id) ON DELETE CASCADE,
    clip_id UUID NOT NULL REFERENCES minigames.clips(id),
    user_id UUID,
    guest_session_id VARCHAR(64),
    guessed_premier_rating INT NOT NULL,
    points_awarded INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_challenge_clip UNIQUE (challenge_id, clip_id, user_id)
);

-- Indexes for frequent queries
CREATE INDEX IF NOT EXISTS idx_clips_status ON minigames.clips(status);
CREATE INDEX IF NOT EXISTS idx_clips_submitted_by ON minigames.clips(submitted_by_user_id);
CREATE INDEX IF NOT EXISTS idx_daily_challenges_date ON minigames.daily_challenges(challenge_date);
CREATE INDEX IF NOT EXISTS idx_user_guesses_challenge_user ON minigames.user_guesses(challenge_id, user_id);
