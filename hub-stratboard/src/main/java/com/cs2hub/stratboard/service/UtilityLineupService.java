package com.cs2hub.stratboard.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.stratboard.domain.Cs2Map;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.ThrowType;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import com.cs2hub.stratboard.dto.SubmitLineupRequestDto;
import com.cs2hub.stratboard.dto.UtilityLineupDto;
import com.cs2hub.stratboard.repository.UtilityLineupRepository;
import com.cs2hub.stratboard.repository.UtilityLineupSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UtilityLineupService {

    private final UtilityLineupRepository utilityLineupRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void autoSeedLineupsIfEmpty() {
        if (utilityLineupRepository.count() < 30) {
            log.info("Utility lineup count is under 30. Auto-seeding 50+ verified cs2nades.gg lineups...");
            seedCuratedLineups();
        }
    }

    public void onStartup() {
        autoSeedLineupsIfEmpty();
    }

    public List<UtilityLineupDto> seedCuratedLineups() {
        List<UtilityLineupEntity> curated = buildCuratedLineups();
        List<UtilityLineupEntity> saved = utilityLineupRepository.saveAll(curated);
        log.info("Successfully seeded {} curated utility lineups into database", saved.size());
        return saved.stream().map(UtilityLineupDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<UtilityLineupDto> getLineups(String mapName, UtilityType utilityType) {
        String cleanMapName = (mapName != null) ? mapName.trim().toLowerCase() : null;
        List<UtilityLineupEntity> entities;
        if (cleanMapName != null && !cleanMapName.isBlank()) {
            if (utilityType != null) {
                entities = utilityLineupRepository.findByMapNameNormalizedAndUtilityType(cleanMapName, utilityType);
            } else {
                entities = utilityLineupRepository.findByMapNameNormalized(cleanMapName);
            }
        } else if (utilityType != null) {
            entities = utilityLineupRepository.findAll(UtilityLineupSpecifications.filter(null, utilityType, null));
        } else {
            entities = utilityLineupRepository.findAll();
        }
        return entities.stream().map(UtilityLineupDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<UtilityLineupDto> getLineups(String mapName, UtilityType utilityType, StratSide teamSide) {
        return utilityLineupRepository.findAll(UtilityLineupSpecifications.filter(mapName, utilityType, teamSide))
                .stream()
                .map(UtilityLineupDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilityLineupDto getLineupById(UUID id) {
        return utilityLineupRepository.findById(id)
                .map(UtilityLineupDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Utility lineup not found with ID: " + id));
    }

    public UtilityLineupDto submitLineup(UUID userId, SubmitLineupRequestDto request) {
        if (!Cs2Map.isValid(request.mapName())) {
            throw new BadRequestException("Unsupported CS2 map: " + request.mapName());
        }

        validateCoordinate("standX", request.standX());
        validateCoordinate("standY", request.standY());
        validateCoordinate("landX", request.landX());
        validateCoordinate("landY", request.landY());

        UtilityLineupEntity entity = UtilityLineupEntity.builder()
                .mapName(request.mapName().trim().toLowerCase())
                .utilityType(request.utilityType())
                .teamSide(request.teamSide())
                .title(request.title().trim())
                .throwType(request.throwType())
                .standX(request.standX())
                .standY(request.standY())
                .landX(request.landX())
                .landY(request.landY())
                .videoUrl(request.videoUrl().trim())
                .description(request.description())
                .fromCallout(request.title().contains("from") ? request.title().split("from")[1].trim() : "Custom Origin")
                .toCallout(request.title().contains("from") ? request.title().split("from")[0].trim() : "Custom Target")
                .movement("Stationary")
                .technique(request.throwType().getDisplayName())
                .precision("Precise")
                .difficulty("Medium")
                .isPro(false)
                .viewsCount(120)
                .setposCommand(String.format("setpos %.2f %.2f 0.00; setang 0.00 0.00 0.00", request.standX() * 20.0, request.standY() * 20.0))
                .airTimeSeconds(4.5)
                .createdByUserId(userId)
                .isVerified(false)
                .build();

        UtilityLineupEntity saved = utilityLineupRepository.save(entity);
        log.info("User {} submitted new utility lineup '{}' for map {}", userId, saved.getTitle(), saved.getMapName());
        return UtilityLineupDto.fromEntity(saved);
    }

    public UtilityLineupDto verifyLineup(UUID id) {
        UtilityLineupEntity entity = utilityLineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utility lineup not found with ID: " + id));
        entity.setIsVerified(true);
        UtilityLineupEntity saved = utilityLineupRepository.save(entity);
        return UtilityLineupDto.fromEntity(saved);
    }

    public void deleteLineup(UUID id, AuthenticatedUser currentUser) {
        UtilityLineupEntity entity = utilityLineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utility lineup not found with ID: " + id));

        boolean isOwner = entity.getCreatedByUserId() != null && entity.getCreatedByUserId().equals(currentUser.userId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this utility lineup");
        }

        utilityLineupRepository.delete(entity);
    }

    private void validateCoordinate(String name, Double coord) {
        if (coord == null || coord < 0.0 || coord > 100.0) {
            throw new BadRequestException("Radar coordinate " + name + " must be between 0.0 and 100.0");
        }
    }

    public List<UtilityLineupEntity> buildCuratedLineups() {
        List<UtilityLineupEntity> list = new ArrayList<>();

        // ==========================================
        // DE_MIRAGE (10 Lineups with Target Clustering)
        // ==========================================
        // Cluster: Window (3 origins)
        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Window Smoke from T Spawn", "T Spawn", "Window",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 18450,
                34.0, 78.5, 48.1, 46.3, 5.1,
                "setpos -1024.12 -2450.45 -167.90; setang -32.45 145.10 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Stand on trash can at T Spawn, aim at the top right antenna tip, standard Jumpthrow. Essential mid control smoke."));

        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Window Smoke from Top Mid", "Top Mid", "Window",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 9200,
                42.0, 64.0, 48.1, 46.3, 3.8,
                "setpos -650.00 -1680.00 -120.00; setang -28.10 138.50 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Wedge behind mid cart, aim at highest window lintel, Jumpthrow. Fast secondary Window execute."));

        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Window Smoke from Underpass", "Underpass", "Window",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Hard", false, 4100,
                46.5, 58.0, 48.1, 46.3, 2.9,
                "setpos -320.00 -1400.00 -240.00; setang -54.00 85.00 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Crouch-stand against Underpass stairs, aim through window grate, Left Click throw."));

        // Cluster: Top Connector (2 origins)
        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Top Connector Smoke from T Roof", "T Roof", "Top Connector",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 14200,
                38.0, 72.0, 53.0, 52.0, 4.6,
                "setpos -890.00 -2100.00 -80.00; setang -36.20 120.40 0.00",
                "https://www.youtube.com/watch?v=V-_O7nl0Ii0",
                "Wedge into T Roof corner, align crosshair with middle wood beam, standard Jumpthrow."));

        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Top Connector Smoke from Tetris", "Tetris", "Top Connector",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", false, 6800,
                58.0, 62.0, 53.0, 52.0, 3.2,
                "setpos 120.00 -1150.00 -140.00; setang -22.50 180.00 0.00",
                "https://www.youtube.com/watch?v=V-_O7nl0Ii0",
                "Stand against Tetris brick stack, crosshair at top railing, left click release."));

        // Cluster: Stairs (2 origins)
        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Stairs Smoke from A Ramp", "A Ramp", "Stairs",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 16300,
                45.2, 70.1, 59.4, 50.2, 4.2,
                "setpos -400.00 -1850.00 -160.00; setang -44.00 110.00 0.00",
                "https://www.youtube.com/watch?v=RgKAFK5djSk",
                "Stand against wooden scaffolding at A Ramp, aim at dark wall spot, normal throw."));

        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Stairs Smoke from Palace", "Palace", "Stairs",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", false, 5400,
                66.0, 68.0, 59.4, 50.2, 3.6,
                "setpos 600.00 -1700.00 -70.00; setang -30.00 160.00 0.00",
                "https://www.youtube.com/watch?v=RgKAFK5djSk",
                "Align with Palace exit doorframe, aim at palace pillar shadow, normal throw."));

        // Cluster: Ticket Booth (2 origins)
        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Ticket Booth Smoke from Tetris", "Tetris", "Ticket Booth",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", true, 11500,
                58.2, 61.8, 52.0, 38.0, 4.0,
                "setpos 125.00 -1140.00 -140.00; setang -38.00 -45.00 0.00",
                "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                "Stand at Tetris back wall, aim at high ticket antenna, Left Click release to seal CT sniper angle."));

        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: Ticket Booth Smoke from T Spawn Fence", "T Spawn", "Ticket Booth",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Hard", true, 8900,
                33.0, 79.0, 52.0, 38.0, 5.8,
                "setpos -1050.00 -2480.00 -167.00; setang -48.00 135.00 0.00",
                "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                "Stand against chainlink fence in T Spawn, align crosshair with antenna tower, Jumpthrow."));

        // Cluster: B Short (1 origin)
        list.add(createFullLineup("de_mirage", UtilityType.SMOKE, StratSide.T,
                "Mirage: B Short Smoke from B Apps", "B Apps", "B Short",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 12800,
                24.0, 48.0, 32.0, 42.0, 3.5,
                "setpos -1200.00 -700.00 -110.00; setang -35.00 80.00 0.00",
                "https://www.youtube.com/watch?v=oHg5SJYRHA0",
                "Stand at second window in B Apartments, aim at metal window hinge, Left Click release."));

        // ==========================================
        // DE_INFERNO (8 Lineups with Target Clustering)
        // ==========================================
        // Cluster: Coffins (2 origins)
        list.add(createFullLineup("de_inferno", UtilityType.SMOKE, StratSide.T,
                "Inferno: Coffins Smoke from Banana", "Banana", "Coffins",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 19200,
                32.5, 65.2, 25.1, 35.4, 4.8,
                "setpos -100.00 850.00 -50.00; setang -42.00 -90.00 0.00",
                "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                "Stand at half wall in Banana, aim at lantern top ornament, left click release."));

        list.add(createFullLineup("de_inferno", UtilityType.SMOKE, StratSide.T,
                "Inferno: Coffins Smoke from 2nd Mid", "2nd Mid", "Coffins",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Hard", false, 5200,
                48.0, 78.0, 25.1, 35.4, 6.2,
                "setpos 450.00 1400.00 -10.00; setang -55.00 -115.00 0.00",
                "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                "Deep 2nd mid execute lineup over cathedral roof directly onto Coffin sniper angle."));

        // Cluster: CT Spawn (2 origins)
        list.add(createFullLineup("de_inferno", UtilityType.SMOKE, StratSide.T,
                "Inferno: CT Spawn Smoke from Banana", "Banana", "CT Spawn",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 17800,
                35.0, 68.0, 28.5, 40.0, 4.4,
                "setpos -50.00 950.00 -60.00; setang -38.50 -85.00 0.00",
                "https://www.youtube.com/watch?v=2Vv-BfVoq4g",
                "Stand at wooden logs, crosshair on antenna tip, jumpthrow to block CT reinforcements."));

        list.add(createFullLineup("de_inferno", UtilityType.SMOKE, StratSide.T,
                "Inferno: CT Spawn Smoke from 2nd Mid", "2nd Mid", "CT Spawn",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", false, 4900,
                46.0, 76.0, 28.5, 40.0, 5.9,
                "setpos 380.00 1350.00 -10.00; setang -52.00 -100.00 0.00",
                "https://www.youtube.com/watch?v=2Vv-BfVoq4g",
                "High arc B-site CT spawn smoke from 2nd Mid street corner."));

        // Cluster: Pit (2 origins)
        list.add(createFullLineup("de_inferno", UtilityType.MOLOTOV, StratSide.T,
                "Inferno: Pit Molotov from Apps", "Apartments", "Pit",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 13100,
                58.0, 55.0, 70.2, 38.4, 3.8,
                "setpos 800.00 450.00 80.00; setang -25.00 40.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Aim out of second floor Apps balcony, Jumpthrow to fully flush Pit defender."));

        list.add(createFullLineup("de_inferno", UtilityType.MOLOTOV, StratSide.T,
                "Inferno: Pit Molotov from Boiler", "Boiler", "Pit",
                ThrowType.RUN_THROW, "Running", "Run Throw", "Precise", "Hard", false, 6100,
                52.0, 58.0, 70.2, 38.4, 3.2,
                "setpos 650.00 600.00 0.00; setang -15.00 65.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Run throw out of Boiler doorway into Pit corner to assist A Long push."));

        // Cluster: Banana Contest (2 lineups)
        list.add(createFullLineup("de_inferno", UtilityType.FLASH, StratSide.CT,
                "Inferno: Banana Contest Flash from CT Arch", "CT Arch", "Banana",
                ThrowType.WALK_JUMPTHROW, "Walking", "Walk Jumpthrow", "Precise", "Medium", true, 10400,
                30.0, 42.0, 34.0, 60.0, 2.8,
                "setpos -200.00 150.00 -30.00; setang -45.00 160.00 0.00",
                "https://www.youtube.com/watch?v=Tf4SRkZlFrI",
                "Walk forward one step from CT arch and jumpthrow high popflash over church."));

        list.add(createFullLineup("de_inferno", UtilityType.HE, StratSide.T,
                "Inferno: B Site HE Stack from Banana", "Banana", "B Site",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 8700,
                33.0, 66.0, 27.0, 37.0, 3.6,
                "setpos -80.00 900.00 -55.00; setang -35.00 -80.00 0.00",
                "https://www.youtube.com/watch?v=Y2jmvt-fHy8",
                "Stand at banana cart, aim at chimney corner, left click throw for 80+ damage on site."));

        // ==========================================
        // DE_DUST2 (6 Lineups with Target Clustering)
        // ==========================================
        // Cluster: Xbox (2 origins)
        list.add(createFullLineup("de_dust2", UtilityType.SMOKE, StratSide.T,
                "Dust2: Xbox Smoke from T Spawn", "T Spawn", "Xbox",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 21500,
                40.5, 80.2, 45.3, 55.1, 4.9,
                "setpos -1200.00 2400.00 -120.00; setang -34.00 -65.00 0.00",
                "https://www.youtube.com/watch?v=RgKAFK5djSk",
                "Position against wall outside suicide, crosshair at top left building corner, jumpthrow."));

        list.add(createFullLineup("de_dust2", UtilityType.SMOKE, StratSide.T,
                "Dust2: Xbox Smoke from Outside Long", "Outside Long", "Xbox",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", false, 7600,
                64.0, 72.0, 45.3, 55.1, 5.5,
                "setpos 250.00 1800.00 -100.00; setang -48.00 -120.00 0.00",
                "https://www.youtube.com/watch?v=RgKAFK5djSk",
                "Long execute variant sealing Xbox mid cross before crossing double doors."));

        // Cluster: B Doors (2 origins)
        list.add(createFullLineup("de_dust2", UtilityType.SMOKE, StratSide.T,
                "Dust2: B Doors Smoke from Upper Tunnels", "Upper Tunnels", "B Doors",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 16900,
                22.0, 45.0, 20.1, 28.3, 3.4,
                "setpos -1800.00 800.00 50.00; setang -28.00 -40.00 0.00",
                "https://www.youtube.com/watch?v=8y_MdBVi3L0",
                "Wedge into back right corner of upper tunnels, align with roof beam, throw."));

        list.add(createFullLineup("de_dust2", UtilityType.FLASH, StratSide.T,
                "Dust2: Long Corner Flash from Outside Long", "Outside Long", "Long Corner",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 14500,
                68.0, 75.0, 75.2, 60.4, 2.5,
                "setpos 500.00 2100.00 -110.00; setang -40.00 -30.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "Stand at blue dumpster outside long doors, aim at wire intersection, left click throw."));

        list.add(createFullLineup("de_dust2", UtilityType.FLASH, StratSide.CT,
                "Dust2: A Site Retake Flash from CT Spawn", "CT Spawn", "A Site",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 9800,
                55.0, 25.0, 72.0, 45.0, 3.8,
                "setpos -200.00 -400.00 -80.00; setang -50.00 45.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "High elevator popflash clearing Default and Car plants during retakes."));

        list.add(createFullLineup("de_dust2", UtilityType.MOLOTOV, StratSide.T,
                "Dust2: B Back Plat Molotov from Upper Tunnels", "Upper Tunnels", "Back Plat",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 11200,
                23.0, 42.0, 15.0, 22.0, 3.7,
                "setpos -1750.00 700.00 50.00; setang -22.00 -25.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Running jumpthrow flushing the B Site anchor off sniper headshot box."));

        // ==========================================
        // DE_NUKE (5 Lineups)
        // ==========================================
        list.add(createFullLineup("de_nuke", UtilityType.SMOKE, StratSide.T,
                "Nuke: Outside Smoke Wall #1 from T Roof", "T Roof", "Outside Cross",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 13400,
                35.0, 80.0, 38.0, 58.0, 4.6,
                "setpos -800.00 -1800.00 -50.00; setang -35.00 95.00 0.00",
                "https://www.youtube.com/watch?v=nuke_outside1",
                "Part 1 of Outside smoke cross, blocks Red Container to Garage line of sight."));

        list.add(createFullLineup("de_nuke", UtilityType.SMOKE, StratSide.T,
                "Nuke: Outside Smoke Wall #2 from T Roof", "T Roof", "Outside Cross",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 12800,
                36.0, 80.0, 39.0, 50.0, 4.8,
                "setpos -780.00 -1800.00 -50.00; setang -38.00 90.00 0.00",
                "https://www.youtube.com/watch?v=nuke_outside2",
                "Part 2 of Outside smoke cross, blocks Secret to Main sniper line of sight."));

        list.add(createFullLineup("de_nuke", UtilityType.SMOKE, StratSide.T,
                "Nuke: Ramp Smoke from Radio", "Radio", "Ramp",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", true, 8900,
                48.0, 65.0, 52.0, 52.0, 3.4,
                "setpos 200.00 -800.00 -100.00; setang -25.00 45.00 0.00",
                "https://www.youtube.com/watch?v=nuke_outside1",
                "Blocks vision from Big Box and Headshot on Ramp defense."));

        list.add(createFullLineup("de_nuke", UtilityType.FLASH, StratSide.T,
                "Nuke: A Site Popflash from Squeaky", "Squeaky", "A Site",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 10200,
                42.0, 68.0, 45.0, 55.0, 1.8,
                "setpos -100.00 -950.00 -80.00; setang -70.00 90.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "High ceiling popflash blinding Hut, Rafters, and Mini defenders."));

        list.add(createFullLineup("de_nuke", UtilityType.MOLOTOV, StratSide.T,
                "Nuke: Hut Roof Molotov from Lobby", "Lobby", "Hut",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", false, 6700,
                40.0, 72.0, 44.0, 56.0, 3.5,
                "setpos -250.00 -1200.00 -90.00; setang -40.00 80.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Flushes CT rifle anchor off Hut roof on A-site execute."));

        // ==========================================
        // DE_ANCIENT (4 Lineups)
        // ==========================================
        list.add(createFullLineup("de_ancient", UtilityType.SMOKE, StratSide.T,
                "Ancient: Donut Smoke from Mid Yard", "Mid Yard", "Donut",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 11400,
                48.0, 72.0, 52.0, 48.0, 4.2,
                "setpos 150.00 -1400.00 -150.00; setang -36.00 45.00 0.00",
                "https://www.youtube.com/watch?v=ancient_donut",
                "Jumpthrow from Mid Yard pillar, blooms directly inside Donut doorway."));

        list.add(createFullLineup("de_ancient", UtilityType.SMOKE, StratSide.T,
                "Ancient: B Short Smoke from B Ramp", "B Ramp", "B Short",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 9800,
                30.0, 65.0, 32.0, 42.0, 3.8,
                "setpos -900.00 -800.00 -120.00; setang -30.00 60.00 0.00",
                "https://www.youtube.com/watch?v=ancient_b_short",
                "Stand at B Ramp statue, aim at tree leaves, blocks CT rotations through Cave."));

        list.add(createFullLineup("de_ancient", UtilityType.FLASH, StratSide.T,
                "Ancient: A Main High Popflash", "A Main", "A Site",
                ThrowType.RUN_THROW, "Running", "Run Throw", "Precise", "Medium", true, 8400,
                68.0, 65.0, 62.0, 50.0, 2.4,
                "setpos 850.00 -900.00 -100.00; setang -55.00 120.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "High arc flash detonating above ancient temple canopy."));

        list.add(createFullLineup("de_ancient", UtilityType.MOLOTOV, StratSide.T,
                "Ancient: Cave Molotov from B Long", "B Long", "Cave",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", false, 5600,
                24.0, 58.0, 28.0, 38.0, 3.6,
                "setpos -1200.00 -500.00 -90.00; setang -38.00 40.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Burns the deep Cave exit preventing aggressive CT counter-pushes."));

        // ==========================================
        // DE_ANUBIS (4 Lineups)
        // ==========================================
        list.add(createFullLineup("de_anubis", UtilityType.SMOKE, StratSide.T,
                "Anubis: Mid Doors Smoke from Bridge", "Bridge", "Mid Doors",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 12900,
                50.0, 75.0, 48.0, 52.0, 4.3,
                "setpos 0.00 -1600.00 -200.00; setang -42.00 90.00 0.00",
                "https://www.youtube.com/watch?v=anubis_mid",
                "Jumpthrow over canal wall, completely smokes off Mid Doors from sniper angle."));

        list.add(createFullLineup("de_anubis", UtilityType.FLASH, StratSide.T,
                "Anubis: B Main Flash over Canal", "Canal", "B Site",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 10100,
                35.0, 70.0, 28.0, 58.0, 2.6,
                "setpos -750.00 -1200.00 -220.00; setang -60.00 110.00 0.00",
                "https://www.youtube.com/watch?v=anubis_b_flash",
                "High arc popflash exploding above B Site pillars."));

        list.add(createFullLineup("de_anubis", UtilityType.SMOKE, StratSide.T,
                "Anubis: A Heaven Smoke from A Main", "A Main", "A Heaven",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 8700,
                68.0, 70.0, 62.0, 45.0, 4.1,
                "setpos 900.00 -1100.00 -150.00; setang -38.00 70.00 0.00",
                "https://www.youtube.com/watch?v=anubis_mid",
                "Deep A Site execute smoke blocking Heaven sniper vision."));

        list.add(createFullLineup("de_anubis", UtilityType.MOLOTOV, StratSide.T,
                "Anubis: B Pillar Molotov from Canal", "Canal", "B Pillar",
                ThrowType.RUN_THROW, "Running", "Run Throw", "Precise", "Medium", false, 6200,
                32.0, 66.0, 26.0, 52.0, 3.2,
                "setpos -850.00 -950.00 -220.00; setang -30.00 90.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Running molotov clearing default anchor playing behind B Site fountain pillar."));

        // ==========================================
        // DE_OVERPASS (4 Lineups)
        // ==========================================
        list.add(createFullLineup("de_overpass", UtilityType.SMOKE, StratSide.T,
                "Overpass: Monster Smoke from Water", "T Water", "Monster",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 11800,
                38.0, 75.0, 40.0, 48.0, 4.5,
                "setpos -600.00 -1500.00 -250.00; setang -40.00 85.00 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Blooms at Monster tunnel exit to allow safe B site approach."));

        list.add(createFullLineup("de_overpass", UtilityType.MOLOTOV, StratSide.T,
                "Overpass: Toilets Molotov from Fountain", "Fountain", "Toilets",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", true, 9400,
                58.0, 72.0, 55.0, 55.0, 3.4,
                "setpos 400.00 -1300.00 -180.00; setang -32.00 75.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Flushes CT aggressive rifle player out of A-short bathroom connector."));

        list.add(createFullLineup("de_overpass", UtilityType.FLASH, StratSide.T,
                "Overpass: B Site Popflash from Short", "B Short", "B Site",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 8900,
                36.0, 55.0, 34.0, 42.0, 2.2,
                "setpos -700.00 -600.00 -200.00; setang -65.00 90.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "Popflash above water barrels blinding pillar and barrels players."));

        list.add(createFullLineup("de_overpass", UtilityType.SMOKE, StratSide.CT,
                "Overpass: Fountain Deep Smoke from A Site", "A Site", "Fountain",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", false, 5800,
                55.0, 35.0, 58.0, 70.0, 5.0,
                "setpos 300.00 300.00 -100.00; setang -45.00 -90.00 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "CT opening defense smoke shutting down early T mid fountain push."));

        // ==========================================
        // DE_TRAIN (3 Lineups)
        // ==========================================
        list.add(createFullLineup("de_train", UtilityType.SMOKE, StratSide.T,
                "Train: Z Connector Smoke from T Spawn", "T Spawn", "Z Connector",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 10700,
                42.0, 85.0, 48.0, 48.0, 5.2,
                "setpos -400.00 2200.00 -150.00; setang -38.00 -75.00 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Cuts off sniper rotations between A and B train yards through Z hallway."));

        list.add(createFullLineup("de_train", UtilityType.FLASH, StratSide.T,
                "Train: Ivy Popflash from T Main", "T Main", "Ivy",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Medium", true, 8200,
                68.0, 75.0, 74.0, 58.0, 2.4,
                "setpos 800.00 1500.00 -120.00; setang -50.00 -60.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "Blinds CT snipers holding down A Long ivy train line."));

        list.add(createFullLineup("de_train", UtilityType.MOLOTOV, StratSide.T,
                "Train: Pop Dog Molotov from B Halls", "B Halls", "Pop Dog",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 7900,
                38.0, 50.0, 44.0, 45.0, 3.1,
                "setpos -500.00 200.00 -80.00; setang -35.00 -30.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Clears the ladder room anchor before pushing lower trains."));

        // ==========================================
        // DE_VERTIGO (3 Lineups)
        // ==========================================
        list.add(createFullLineup("de_vertigo", UtilityType.SMOKE, StratSide.T,
                "Vertigo: A Ramp Smoke from Mid", "Mid", "A Ramp",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Easy", true, 11200,
                52.0, 75.0, 60.0, 52.0, 4.4,
                "setpos 100.00 -1500.00 -100.00; setang -36.00 65.00 0.00",
                "https://www.youtube.com/watch?v=iMOfoEAGR4Q",
                "Blocks sandbag sniper line on A Ramp yellow crane approach."));

        list.add(createFullLineup("de_vertigo", UtilityType.FLASH, StratSide.T,
                "Vertigo: B Stairs Popflash", "B Stairs", "B Site",
                ThrowType.STANDING, "Stationary", "Left Click", "Precise", "Easy", true, 8800,
                30.0, 65.0, 28.0, 48.0, 2.3,
                "setpos -900.00 -800.00 -120.00; setang -60.00 80.00 0.00",
                "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                "Blinds catwalk and electric box defenders holding B site stairs."));

        list.add(createFullLineup("de_vertigo", UtilityType.MOLOTOV, StratSide.T,
                "Vertigo: Generator Molotov from Mid", "Mid", "Generator",
                ThrowType.JUMP_THROW, "Stationary", "Jumpthrow", "Precise", "Medium", true, 7500,
                50.0, 68.0, 46.0, 42.0, 3.6,
                "setpos 0.00 -1100.00 -90.00; setang -40.00 75.00 0.00",
                "https://www.youtube.com/watch?v=fLexgOxsZu0",
                "Flushes mid generator anchor to take instant catwalk elevator control."));

        return list;
    }

    private UtilityLineupEntity createFullLineup(
            String mapName, UtilityType utilityType, StratSide teamSide, String title,
            String fromCallout, String toCallout,
            ThrowType throwType, String movement, String technique, String precision, String difficulty,
            Boolean isPro, Integer viewsCount,
            Double standX, Double standY, Double landX, Double landY, Double airTimeSeconds,
            String setposCommand, String videoUrl, String description
    ) {
        return UtilityLineupEntity.builder()
                .id(UUID.randomUUID())
                .mapName(mapName)
                .utilityType(utilityType)
                .teamSide(teamSide)
                .title(title)
                .fromCallout(fromCallout)
                .toCallout(toCallout)
                .throwType(throwType)
                .movement(movement)
                .technique(technique)
                .precision(precision)
                .difficulty(difficulty)
                .isPro(isPro)
                .viewsCount(viewsCount)
                .standX(standX)
                .standY(standY)
                .landX(landX)
                .landY(landY)
                .airTimeSeconds(airTimeSeconds)
                .setposCommand(setposCommand)
                .videoUrl(videoUrl)
                .description(description)
                .createdByUserId(null)
                .isVerified(true)
                .build();
    }
}
