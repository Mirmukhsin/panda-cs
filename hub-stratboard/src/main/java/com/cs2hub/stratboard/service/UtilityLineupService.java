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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UtilityLineupService {

    private final UtilityLineupRepository utilityLineupRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (utilityLineupRepository.count() == 0) {
            log.info("No utility lineups found in database. Initializing curated CS2 utility lineup seed library...");
            seedCuratedLineups();
        }
    }

    public List<UtilityLineupDto> seedCuratedLineups() {
        List<UtilityLineupEntity> curated = buildCuratedLineups();
        List<UtilityLineupEntity> saved = utilityLineupRepository.saveAll(curated);
        log.info("Successfully seeded {} curated utility lineups into database", saved.size());
        return saved.stream().map(UtilityLineupDto::fromEntity).toList();
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
        return List.of(
                // Mirage (5)
                createLineup("de_mirage", UtilityType.SMOKE, StratSide.T, "Mirage Window Smoke from T Spawn",
                        ThrowType.JUMP_THROW, 34.0, 78.0, 48.0, 46.0,
                        "https://youtube.com/watch?v=mirage_window", "Align with trash can near T Spawn, crosshair on top-right antenna corner, D+Jumpthrow"),

                createLineup("de_mirage", UtilityType.SMOKE, StratSide.T, "Mirage Connector Smoke from T Roof",
                        ThrowType.JUMP_THROW, 38.0, 72.0, 53.0, 52.0,
                        "https://youtube.com/watch?v=mirage_conn", "Stand against T Roof corner, aim at the middle wood peg, standard Jumpthrow"),

                createLineup("de_mirage", UtilityType.SMOKE, StratSide.T, "Mirage Stairs Smoke from A Ramp",
                        ThrowType.STANDING, 45.0, 70.0, 59.0, 50.0,
                        "https://youtube.com/watch?v=mirage_stairs", "Position against A Ramp wood board, aim at the dark spot on wall, Left-click throw"),

                createLineup("de_mirage", UtilityType.SMOKE, StratSide.T, "Mirage Jungle Smoke from A Ramp",
                        ThrowType.STANDING, 44.0, 71.0, 56.0, 47.0,
                        "https://youtube.com/watch?v=mirage_jungle", "Stand on A Ramp center, aim between the two pillar tips, Left-click throw"),

                createLineup("de_mirage", UtilityType.FLASH, StratSide.T, "Mirage A-Site Deep Flash from Palace",
                        ThrowType.RUN_THROW, 65.0, 68.0, 60.0, 58.0,
                        "https://youtube.com/watch?v=mirage_flash", "Running flash out of Palace window blinds all CT players holding Site and CT spawn"),

                // Inferno (4)
                createLineup("de_inferno", UtilityType.SMOKE, StratSide.T, "Inferno Coffin Smoke from Banana",
                        ThrowType.STANDING, 32.0, 65.0, 25.0, 35.0,
                        "https://youtube.com/watch?v=inferno_coffin", "Stand at half wall in Banana, aim at lantern top, Left-click throw"),

                createLineup("de_inferno", UtilityType.SMOKE, StratSide.T, "Inferno CT Cross Smoke from Banana",
                        ThrowType.JUMP_THROW, 35.0, 68.0, 28.0, 40.0,
                        "https://youtube.com/watch?v=inferno_ct", "Stand at wooden barrels, aim at the antenna corner, Jumpthrow"),

                createLineup("de_inferno", UtilityType.MOLOTOV, StratSide.T, "Inferno Car Molotov from Banana",
                        ThrowType.RUN_THROW, 40.0, 75.0, 36.0, 62.0,
                        "https://youtube.com/watch?v=inferno_car", "Running molotov clearing Sandbags and Car from Banana ramp"),

                createLineup("de_inferno", UtilityType.MOLOTOV, StratSide.T, "Inferno Pit Molotov from Apps",
                        ThrowType.JUMP_THROW, 58.0, 55.0, 70.0, 38.0,
                        "https://youtube.com/watch?v=inferno_pit", "Aim out of second floor Apps balcony, Jumpthrow to fully flush Pit defender"),

                // Dust II (3)
                createLineup("de_dust2", UtilityType.SMOKE, StratSide.T, "Dust II Xbox Smoke from T Spawn",
                        ThrowType.JUMP_THROW, 40.0, 80.0, 45.0, 55.0,
                        "https://youtube.com/watch?v=d2_xbox", "Stand outside T Spawn tunnel, aim at the wall stain, Jumpthrow to seal Catwalk cross"),

                createLineup("de_dust2", UtilityType.FLASH, StratSide.T, "Dust II Long Corner Flash from Outside Long",
                        ThrowType.STANDING, 68.0, 75.0, 75.0, 60.0,
                        "https://youtube.com/watch?v=d2_long_flash", "Pop flash over Long Doors building, blinding CTs holding A Long corner"),

                createLineup("de_dust2", UtilityType.SMOKE, StratSide.T, "Dust II B Door Smoke from Upper Tunnels",
                        ThrowType.STANDING, 22.0, 45.0, 20.0, 28.0,
                        "https://youtube.com/watch?v=d2_b_doors", "Align in dark corner of Upper Tunnels, aim at beam junction, Left-click throw"),

                // Nuke (2)
                createLineup("de_nuke", UtilityType.SMOKE, StratSide.T, "Nuke Outside Smoke Wall #1 from T Roof",
                        ThrowType.JUMP_THROW, 35.0, 80.0, 38.0, 58.0,
                        "https://youtube.com/watch?v=nuke_outside1", "Part 1 of Outside smoke cross, blocks Red Container to Garage line of sight"),

                createLineup("de_nuke", UtilityType.SMOKE, StratSide.T, "Nuke Outside Smoke Wall #2 from T Roof",
                        ThrowType.JUMP_THROW, 36.0, 80.0, 39.0, 50.0,
                        "https://youtube.com/watch?v=nuke_outside2", "Part 2 of Outside smoke cross, blocks Secret to Main sniper line of sight"),

                // Ancient (2)
                createLineup("de_ancient", UtilityType.SMOKE, StratSide.T, "Ancient Donut Smoke from Mid Yard",
                        ThrowType.JUMP_THROW, 48.0, 72.0, 52.0, 48.0,
                        "https://youtube.com/watch?v=ancient_donut", "Jumpthrow from Mid Yard pillar, blooms directly inside Donut doorway"),

                createLineup("de_ancient", UtilityType.SMOKE, StratSide.T, "Ancient B Short Smoke from B Ramp",
                        ThrowType.STANDING, 30.0, 65.0, 32.0, 42.0,
                        "https://youtube.com/watch?v=ancient_b_short", "Stand at B Ramp statue, aim at tree leaves, blocks CT rotations through Cave"),

                // Anubis (2)
                createLineup("de_anubis", UtilityType.SMOKE, StratSide.T, "Anubis Mid Doors Smoke from Bridge",
                        ThrowType.JUMP_THROW, 50.0, 75.0, 48.0, 52.0,
                        "https://youtube.com/watch?v=anubis_mid", "Jumpthrow over canal wall, completely smokes off Mid Doors from sniper angle"),

                createLineup("de_anubis", UtilityType.FLASH, StratSide.T, "Anubis B Main Flash over Canal",
                        ThrowType.STANDING, 35.0, 70.0, 28.0, 58.0,
                        "https://youtube.com/watch?v=anubis_b_flash", "High arc popflash exploding above B Site pillars")
        );
    }

    private UtilityLineupEntity createLineup(
            String mapName, UtilityType utilityType, StratSide teamSide, String title,
            ThrowType throwType, Double standX, Double standY, Double landX, Double landY,
            String videoUrl, String description
    ) {
        return UtilityLineupEntity.builder()
                .id(UUID.randomUUID())
                .mapName(mapName)
                .utilityType(utilityType)
                .teamSide(teamSide)
                .title(title)
                .throwType(throwType)
                .standX(standX)
                .standY(standY)
                .landX(landX)
                .landY(landY)
                .videoUrl(videoUrl)
                .description(description)
                .createdByUserId(null)
                .isVerified(true)
                .build();
    }
}
