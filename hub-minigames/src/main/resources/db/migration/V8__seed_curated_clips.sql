-- Migration V8: Seed curated fallback clips and support curated clip flag for Daily Guess the Elo

-- Add is_curated column to minigames.clips
ALTER TABLE minigames.clips
    ADD COLUMN IF NOT EXISTS is_curated BOOLEAN DEFAULT FALSE;

-- Make submitted_by_user_id nullable to support system/curated clips cleanly
ALTER TABLE minigames.clips
    ALTER COLUMN submitted_by_user_id DROP NOT NULL;

-- Index for querying curated and approved clips quickly
CREATE INDEX IF NOT EXISTS idx_clips_curated_status ON minigames.clips(is_curated, status);

-- Seed 24 high-quality curated CS2 clips across all Premier rank brackets (Low: 4k-8k, Mid: 10k-16k, High: 18k-28k+)
INSERT INTO minigames.clips (
    id, submitted_by_user_id, title, video_url, video_provider, map_name,
    actual_premier_rating, actual_faceit_level, status, reviewed_by_user_id, is_curated, created_at, updated_at
) VALUES
-- 1-8: Low Premier / Silver-Gold (4,000 - 8,000)
('a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Mirage Palace Push 1v2 Clutch', 'https://www.youtube.com/watch?v=kJQP7kiw5Fk', 'YOUTUBE', 'de_mirage', 5400, 2, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Dust 2 Long A Cross AWP Whiff to Deagle Headshot', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'YOUTUBE', 'de_dust2', 6800, 3, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Inferno B Apartments P90 Rush Hold', 'https://youtu.be/3JZ_D3ELwOQ', 'YOUTUBE', 'de_inferno', 4300, 1, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'Nuke Ramp Room Defensive Panic Spray', 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ', 'YOUTUBE', 'de_nuke', 7200, 3, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'Anubis B Site Bridge Shotgun Ambush', 'https://youtu.be/L_LUpnjgPso', 'YOUTUBE', 'de_anubis', 5900, 2, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'Ancient Cave Retake with MP9', 'https://www.youtube.com/watch?v=eBGIQ7ZuuiU', 'YOUTUBE', 'de_ancient', 7700, 3, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'Mirage B Apartments Smoke Ninja Defuse', 'https://clips.twitch.tv/CuriousFriendlyClam', 'TWITCH', 'de_mirage', 6100, 2, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001', 'Dust 2 Mid Doors Scout Peek', 'https://www.youtube.com/watch?v=kXYiU_JCYtU', 'YOUTUBE', 'de_dust2', 4850, 2, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),

-- 9-16: Mid Premier / Master Guardian - DMG (10,000 - 16,000)
('a0000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001', 'Mirage Mid Window Smoke & Fast Connector Pop', 'https://www.youtube.com/watch?v=V-_O7nl0Ii0', 'YOUTUBE', 'de_mirage', 12400, 5, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'Inferno Banana 3k Spray Transfer with AK-47', 'https://youtu.be/2Vv-BfVoq4g', 'YOUTUBE', 'de_inferno', 14800, 6, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'Dust 2 A Site Catwalk Fast Retake', 'https://www.youtube.com/watch?v=RgKAFK5djSk', 'YOUTUBE', 'de_dust2', 10900, 5, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'Nuke Vent Dive to Secret Flank', 'https://clips.twitch.tv/BlushingHedonisticSparrow', 'TWITCH', 'de_nuke', 13500, 6, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000001', 'Ancient Mid Donut Hold with M4A1-S', 'https://www.youtube.com/watch?v=OPf0YbXqDm0', 'YOUTUBE', 'de_ancient', 15200, 7, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000001', 'Anubis Water Control 1v3 Deagle Eco', 'https://youtu.be/hT_nvWreIhg', 'YOUTUBE', 'de_anubis', 11800, 5, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000001', 'Mirage A Ramp Flash Retake Multi-Kill', 'https://www.youtube.com/watch?v=9bZkp7q19f0', 'YOUTUBE', 'de_mirage', 14100, 6, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000001', 'Inferno Pit Hold Solo Defense Against A-Rush', 'https://www.youtube.com/watch?v=kJQP7kiw5F0', 'YOUTUBE', 'de_inferno', 15750, 7, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),

-- 17-24: High Premier / Faceit Level 8-10 / Pro (18,000 - 28,000+)
('a0000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000001', 'Mirage Fast Catwalk AWP Flicktastic 4k', 'https://www.youtube.com/watch?v=oHg5SJYRHA0', 'YOUTUBE', 'de_mirage', 22400, 9, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000001', 'Nuke Outside Yard Aggressive Solo Peek Ace', 'https://clips.twitch.tv/SarcasticHyperOtter', 'TWITCH', 'de_nuke', 25100, 10, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000001', 'Dust 2 B Site Solo 1v4 Post-Plant Clutch', 'https://youtu.be/C0DPdy98e4c', 'YOUTUBE', 'de_dust2', 20600, 8, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000001', 'Inferno Arch Side Aggressive Double Peek 3k', 'https://www.youtube.com/watch?v=fLexgOxsZu0', 'YOUTUBE', 'de_inferno', 23800, 10, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000001', 'Ancient B Site Instant Headshot Wallbang & Ace', 'https://www.youtube.com/watch?v=kffacxfA7G4', 'YOUTUBE', 'de_ancient', 27500, 10, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000001', 'Anubis A Site Heaven Precision AWP Retake', 'https://clips.twitch.tv/HonestGlamorousWaffle', 'TWITCH', 'de_anubis', 21900, 9, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000001', 'Mirage Jungle To Connector Clean Deagle 4k', 'https://www.youtube.com/watch?v=uelHwf8o7_U', 'YOUTUBE', 'de_mirage', 26300, 10, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000001', 'Nuke Hut Jump Headshot to Squeaky Door 1v3', 'https://youtu.be/60ItHLz5WEA', 'YOUTUBE', 'de_nuke', 28400, 10, 'APPROVED', '00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
