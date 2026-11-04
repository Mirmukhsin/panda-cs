-- Flyway Migration: V9__seed_cs2nades_lineups.sql
-- Module: hub-stratboard
-- Purpose: Seed 30+ cs2nades.gg-style utility lineups across Active Duty maps

INSERT INTO stratboard.utility_lineups (
    id, map_name, utility_type, team_side, title, throw_type,
    stand_x, stand_y, land_x, land_y, video_url, description,
    created_by_user_id, is_verified, created_at
) VALUES
-- Mirage (de_mirage)
('b0000000-0000-0000-0000-000000000001', 'de_mirage', 'SMOKE', 'T', 'Mirage: T Spawn → Window Smoke (Jumpthrow)', 'JUMPTHROW',
 34.2, 78.5, 48.1, 46.3, 'https://www.youtube.com/watch?v=kJQP7kiw5Fk', 'Stand on the waste bin in T Spawn, line up crosshair with antenna tip and jumpthrow.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000002', 'de_mirage', 'SMOKE', 'T', 'Mirage: T Roof → Connector Smoke', 'STANDING',
 38.0, 72.0, 53.0, 52.0, 'https://www.youtube.com/watch?v=V-_O7nl0Ii0', 'Wedge into T Roof corner, aim at the middle wooden carpet beam, left click throw.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000003', 'de_mirage', 'SMOKE', 'T', 'Mirage: A Ramp → Stairs Smoke', 'STANDING',
 45.2, 70.1, 59.4, 50.2, 'https://www.youtube.com/watch?v=RgKAFK5djSk', 'Stand against A Ramp wooden scaffolding, aim at the dark spot on wall, normal throw.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000004', 'de_mirage', 'FLASH', 'T', 'Mirage: Palace → A Site Pop Flash', 'RUNNING',
 65.0, 68.0, 60.5, 58.2, 'https://www.youtube.com/watch?v=oHg5SJYRHA0', 'Run throw out of palace balcony to blind site anchors and ticket booth.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000005', 'de_mirage', 'MOLOTOV', 'CT', 'Mirage: CT Spawn → B Apartments Molotov', 'JUMPTHROW',
 52.0, 32.0, 24.5, 36.8, 'https://www.youtube.com/watch?v=uelHwf8o7_U', 'Stand near CT ticket, aim above kitchen roof, jumpthrow to incinerate B rush entrance.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000006', 'de_mirage', 'HE', 'BOTH', 'Mirage: Top Mid → Window HE Grenade', 'CROUCH',
 42.0, 62.0, 48.0, 47.0, 'https://www.youtube.com/watch?v=kXYiU_JCYtU', 'Crouch behind top mid boxes, aim at window sill edge, stand and release.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

-- Inferno (de_inferno)
('b0000000-0000-0000-0000-000000000007', 'de_inferno', 'SMOKE', 'T', 'Inferno: Banana → Coffins Smoke', 'STANDING',
 32.5, 65.2, 25.1, 35.4, 'https://www.youtube.com/watch?v=3JZ_D3ELwOQ', 'Stand at half wall in Banana, aim at lantern top ornament, left click release.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000008', 'de_inferno', 'SMOKE', 'T', 'Inferno: Banana → CT Cross Smoke', 'JUMPTHROW',
 35.0, 68.0, 28.5, 40.0, 'https://www.youtube.com/watch?v=2Vv-BfVoq4g', 'Stand at wooden logs, crosshair on antenna tip, jumpthrow.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000009', 'de_inferno', 'MOLOTOV', 'T', 'Inferno: Banana → Pit Molotov', 'JUMPTHROW',
 58.0, 55.0, 70.2, 38.4, 'https://www.youtube.com/watch?v=fLexgOxsZu0', 'Jumpthrow from boiler side to burn deep pit corner completely.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000010', 'de_inferno', 'MOLOTOV', 'T', 'Inferno: Banana → Car Molotov', 'RUNNING',
 40.2, 75.0, 36.1, 62.3, 'https://www.youtube.com/watch?v=kJQP7kiw5F0', 'Running throw down banana to clear sandbags and car defenders.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000011', 'de_inferno', 'FLASH', 'CT', 'Inferno: CT Arch → Banana Retake Flash', 'WALK_JUMPTHROW',
 30.0, 42.0, 34.0, 60.0, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'Walk forward one step from CT arch and jumpthrow high popflash over church.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000012', 'de_inferno', 'HE', 'BOTH', 'Inferno: Banana → B Site HE Stack', 'STANDING',
 33.0, 66.0, 27.0, 37.0, 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ', 'Stand at banana cart, aim at chimney corner, left click throw for 80+ damage on site.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

-- Dust II (de_dust2)
('b0000000-0000-0000-0000-000000000013', 'de_dust2', 'SMOKE', 'T', 'Dust2: T Spawn → Xbox Smoke', 'JUMPTHROW',
 40.5, 80.2, 45.3, 55.1, 'https://www.youtube.com/watch?v=RgKAFK5djSk', 'Position against wall outside suicide, crosshair at top left building corner, jumpthrow.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000014', 'de_dust2', 'FLASH', 'T', 'Dust2: Outside Long → Long Corner Flash', 'STANDING',
 68.0, 75.0, 75.2, 60.4, 'https://www.youtube.com/watch?v=kXYiU_JCYtU', 'Stand at blue dumpster outside long doors, aim at wire intersection, left click throw.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000015', 'de_dust2', 'SMOKE', 'T', 'Dust2: Upper Tunnels → B Doors Smoke', 'STANDING',
 22.0, 45.0, 20.1, 28.3, 'https://www.youtube.com/watch?v=C0DPdy98e4c', 'Wedge into back right corner of upper tunnels, align with roof beam, throw.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000016', 'de_dust2', 'MOLOTOV', 'CT', 'Dust2: Mid Doors → Short Molotov', 'WALK_JUMPTHROW',
 48.0, 50.0, 52.0, 42.0, 'https://www.youtube.com/watch?v=kJQP7kiw5Fk', 'Take one step forward from CT mid double doors, jumpthrow towards catwalk stairs.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000017', 'de_dust2', 'HE', 'BOTH', 'Dust2: Long Doors → Pit HE Grenade', 'RUNNING',
 69.0, 65.0, 78.0, 48.0, 'https://www.youtube.com/watch?v=OPf0YbXqDm0', 'Running throw through long doors straight into pit to dislodge AWP camper.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000018', 'de_dust2', 'FLASH', 'CT', 'Dust2: CT Mid → Tunnels Exit Flash', 'CROUCH',
 42.0, 48.0, 28.0, 40.0, 'https://www.youtube.com/watch?v=V-_O7nl0Ii0', 'Crouch by mid B doors and throw underhand flash to blind upper tunnel rushers.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

-- Nuke (de_nuke)
('b0000000-0000-0000-0000-000000000019', 'de_nuke', 'SMOKE', 'T', 'Nuke: Silo → Garage Cross Smoke', 'JUMPTHROW',
 35.5, 80.0, 38.2, 58.0, 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ', 'Align on Silo catwalk, aim at crane tip, jumpthrow for outside cross smoke #1.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000020', 'de_nuke', 'SMOKE', 'T', 'Nuke: T Roof → Secret Cross Smoke', 'JUMPTHROW',
 36.5, 80.0, 39.1, 50.0, 'https://www.youtube.com/watch?v=SarcasticHyperOtter', 'Second part of outside smoke wall, blocks Secret sightline from CT main.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000021', 'de_nuke', 'MOLOTOV', 'T', 'Nuke: T Roof → Hut Molotov', 'STANDING',
 44.0, 72.0, 48.0, 56.0, 'https://www.youtube.com/watch?v=60ItHLz5WEA', 'Stand by roof ventilator, aim at skybox antenna, left-click throw to burn inside Hut.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000022', 'de_nuke', 'FLASH', 'T', 'Nuke: Lobby → A Site Pop Flash', 'RUNNING',
 46.0, 68.0, 50.0, 52.0, 'https://www.youtube.com/watch?v=BlushingHedonisticSparrow', 'Running flash through squeaky door ceiling grating, perfectly pops above silo.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000023', 'de_nuke', 'HE', 'CT', 'Nuke: Outside Garage → Secret Retake HE', 'WALK_JUMPTHROW',
 40.0, 54.0, 38.0, 44.0, 'https://www.youtube.com/watch?v=oHg5SJYRHA0', 'Walk jumpthrow over red container into secret stairs to finish low HP bomb planters.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000024', 'de_nuke', 'FLASH', 'BOTH', 'Nuke: Heaven → Ramp Flash', 'CROUCH',
 58.0, 48.0, 64.0, 55.0, 'https://www.youtube.com/watch?v=uelHwf8o7_U', 'Crouch by heaven catwalk railing, drop flash directly into ramp room entrance.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

-- Ancient (de_ancient)
('b0000000-0000-0000-0000-000000000025', 'de_ancient', 'SMOKE', 'T', 'Ancient: T Spawn → Red Room Smoke', 'JUMPTHROW',
 48.0, 72.0, 52.0, 48.0, 'https://www.youtube.com/watch?v=eBGIQ7ZuuiU', 'Stand by T spawn ruins, aim at top foliage vertex, jumpthrow to block Red Room sniper.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000026', 'de_ancient', 'SMOKE', 'T', 'Ancient: Mid Yard → Donut Smoke', 'STANDING',
 50.0, 68.0, 56.0, 52.0, 'https://www.youtube.com/watch?v=OPf0YbXqDm0', 'Align against middle pillar, aim at carving eye on wall, standard standing throw.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000027', 'de_ancient', 'MOLOTOV', 'T', 'Ancient: B Ramp → Cave Molotov', 'RUNNING',
 30.0, 65.0, 32.0, 42.0, 'https://www.youtube.com/watch?v=kffacxfA7G4', 'Running throw down B ramp that spreads across cave entrance and default box.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000028', 'de_ancient', 'FLASH', 'T', 'Ancient: A Main → Temple Entrance Flash', 'JUMPTHROW',
 62.0, 66.0, 68.0, 52.0, 'https://www.youtube.com/watch?v=kJQP7kiw5Fk', 'High popflash over the temple archway blinding all CTs defending A site.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000029', 'de_ancient', 'HE', 'CT', 'Ancient: CT Spawn → Mid Donut HE Grenade', 'WALK_JUMPTHROW',
 52.0, 36.0, 51.0, 46.0, 'https://www.youtube.com/watch?v=2Vv-BfVoq4g', 'Walk jumpthrow through CT connector to explode against mid aggression.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000030', 'de_ancient', 'SMOKE', 'BOTH', 'Ancient: Cheetah → B Site Retake Smoke', 'CROUCH',
 36.0, 50.0, 30.0, 38.0, 'https://www.youtube.com/watch?v=3JZ_D3ELwOQ', 'Crouch behind cheetah pillar, aim at top left moss, stand and throw to extinguish site mollies.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

-- Anubis (de_anubis)
('b0000000-0000-0000-0000-000000000031', 'de_anubis', 'SMOKE', 'T', 'Anubis: B Main → B Connector Smoke', 'JUMPTHROW',
 35.0, 70.0, 28.0, 52.0, 'https://www.youtube.com/watch?v=L_LUpnjgPso', 'Align with palace column in B Main, aim at tower top ornament, jumpthrow.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000032', 'de_anubis', 'SMOKE', 'T', 'Anubis: Canal Bridge → Mid Doors Smoke', 'STANDING',
 50.0, 75.0, 48.0, 52.0, 'https://www.youtube.com/watch?v=hT_nvWreIhg', 'Stand on wooden bridge, aim at the arch corner, standard throw to blind sniper nest.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000033', 'de_anubis', 'MOLOTOV', 'T', 'Anubis: A Main → Heaven Molotov', 'WALK_JUMPTHROW',
 66.0, 72.0, 72.0, 54.0, 'https://www.youtube.com/watch?v=HonestGlamorousWaffle', 'Walk one step and jumpthrow through A Main opening to clear Heaven perch.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000034', 'de_anubis', 'FLASH', 'T', 'Anubis: Canal → B Site Pop Flash', 'RUNNING',
 38.0, 68.0, 32.0, 58.0, 'https://www.youtube.com/watch?v=kJQP7kiw5F0', 'Run throw across water over the canal roof, blinding site pillars and alleyway.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000035', 'de_anubis', 'HE', 'CT', 'Anubis: CT Spawn → Mid Canal HE Grenade', 'JUMPTHROW',
 52.0, 38.0, 49.0, 58.0, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'Jumpthrow from CT spawn stairs to blast water pushers before they round connector.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW()),

('b0000000-0000-0000-0000-000000000036', 'de_anubis', 'SMOKE', 'BOTH', 'Anubis: B Alley → Default Plant Site Smoke', 'CROUCH',
 26.0, 48.0, 28.0, 40.0, 'https://www.youtube.com/watch?v=V-_O7nl0Ii0', 'Crouch under awning, aim at center hieroglyph, release for safe B bomb plant.',
 '00000000-0000-0000-0000-000000000001', TRUE, NOW())

ON CONFLICT (id) DO NOTHING;
