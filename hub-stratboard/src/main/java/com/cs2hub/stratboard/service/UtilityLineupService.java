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

import java.time.Instant;
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
            log.info("Utility lineup count is under 30. Auto-seeding 36 verified cs2nades.gg lineups...");
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
        if (teamSide == null) {
            return getLineups(mapName, utilityType);
        }
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
                // Mirage (de_mirage)
                createLineup("b0000000-0000-0000-0000-000000000001", "de_mirage", UtilityType.SMOKE, StratSide.T,
                        "Mirage: T Spawn \u2192 Window Smoke (Jumpthrow)", ThrowType.JUMPTHROW,
                        34.2, 78.5, 48.1, 46.3,
                        "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
                        "Stand on the waste bin in T Spawn, line up crosshair with antenna tip and jumpthrow."),

                createLineup("b0000000-0000-0000-0000-000000000002", "de_mirage", UtilityType.SMOKE, StratSide.T,
                        "Mirage: T Roof \u2192 Connector Smoke", ThrowType.STANDING,
                        38.0, 72.0, 53.0, 52.0,
                        "https://www.youtube.com/watch?v=V-_O7nl0Ii0",
                        "Wedge into T Roof corner, aim at the middle wooden carpet beam, left click throw."),

                createLineup("b0000000-0000-0000-0000-000000000003", "de_mirage", UtilityType.SMOKE, StratSide.T,
                        "Mirage: A Ramp \u2192 Stairs Smoke", ThrowType.STANDING,
                        45.2, 70.1, 59.4, 50.2,
                        "https://www.youtube.com/watch?v=RgKAFK5djSk",
                        "Stand against A Ramp wooden scaffolding, aim at the dark spot on wall, normal throw."),

                createLineup("b0000000-0000-0000-0000-000000000004", "de_mirage", UtilityType.FLASH, StratSide.T,
                        "Mirage: Palace \u2192 A Site Pop Flash", ThrowType.RUNNING,
                        65.0, 68.0, 60.5, 58.2,
                        "https://www.youtube.com/watch?v=oHg5SJYRHA0",
                        "Run throw out of palace balcony to blind site anchors and ticket booth."),

                createLineup("b0000000-0000-0000-0000-000000000005", "de_mirage", UtilityType.MOLOTOV, StratSide.CT,
                        "Mirage: CT Spawn \u2192 B Apartments Molotov", ThrowType.JUMPTHROW,
                        52.0, 32.0, 24.5, 36.8,
                        "https://www.youtube.com/watch?v=uelHwf8o7_U",
                        "Stand near CT ticket, aim above kitchen roof, jumpthrow to incinerate B rush entrance."),

                createLineup("b0000000-0000-0000-0000-000000000006", "de_mirage", UtilityType.HE, StratSide.BOTH,
                        "Mirage: Top Mid \u2192 Window HE Grenade", ThrowType.CROUCH,
                        42.0, 62.0, 48.0, 47.0,
                        "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                        "Crouch behind top mid boxes, aim at window sill edge, stand and release."),

                // Inferno (de_inferno)
                createLineup("b0000000-0000-0000-0000-000000000007", "de_inferno", UtilityType.SMOKE, StratSide.T,
                        "Inferno: Banana \u2192 Coffins Smoke", ThrowType.STANDING,
                        32.5, 65.2, 25.1, 35.4,
                        "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                        "Stand at half wall in Banana, aim at lantern top ornament, left click release."),

                createLineup("b0000000-0000-0000-0000-000000000008", "de_inferno", UtilityType.SMOKE, StratSide.T,
                        "Inferno: Banana \u2192 CT Cross Smoke", ThrowType.JUMPTHROW,
                        35.0, 68.0, 28.5, 40.0,
                        "https://www.youtube.com/watch?v=2Vv-BfVoq4g",
                        "Stand at wooden logs, crosshair on antenna tip, jumpthrow."),

                createLineup("b0000000-0000-0000-0000-000000000009", "de_inferno", UtilityType.MOLOTOV, StratSide.T,
                        "Inferno: Banana \u2192 Pit Molotov", ThrowType.JUMPTHROW,
                        58.0, 55.0, 70.2, 38.4,
                        "https://www.youtube.com/watch?v=fLexgOxsZu0",
                        "Jumpthrow from boiler side to burn deep pit corner completely."),

                createLineup("b0000000-0000-0000-0000-000000000010", "de_inferno", UtilityType.MOLOTOV, StratSide.T,
                        "Inferno: Banana \u2192 Car Molotov", ThrowType.RUNNING,
                        40.2, 75.0, 36.1, 62.3,
                        "https://www.youtube.com/watch?v=kJQP7kiw5F0",
                        "Running throw down banana to clear sandbags and car defenders."),

                createLineup("b0000000-0000-0000-0000-000000000011", "de_inferno", UtilityType.FLASH, StratSide.CT,
                        "Inferno: CT Arch \u2192 Banana Retake Flash", ThrowType.WALK_JUMPTHROW,
                        30.0, 42.0, 34.0, 60.0,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        "Walk forward one step from CT arch and jumpthrow high popflash over church."),

                createLineup("b0000000-0000-0000-0000-000000000012", "de_inferno", UtilityType.HE, StratSide.BOTH,
                        "Inferno: Banana \u2192 B Site HE Stack", ThrowType.STANDING,
                        33.0, 66.0, 27.0, 37.0,
                        "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
                        "Stand at banana cart, aim at chimney corner, left click throw for 80+ damage on site."),

                // Dust II (de_dust2)
                createLineup("b0000000-0000-0000-0000-000000000013", "de_dust2", UtilityType.SMOKE, StratSide.T,
                        "Dust2: T Spawn \u2192 Xbox Smoke", ThrowType.JUMPTHROW,
                        40.5, 80.2, 45.3, 55.1,
                        "https://www.youtube.com/watch?v=RgKAFK5djSk",
                        "Position against wall outside suicide, crosshair at top left building corner, jumpthrow."),

                createLineup("b0000000-0000-0000-0000-000000000014", "de_dust2", UtilityType.FLASH, StratSide.T,
                        "Dust2: Outside Long \u2192 Long Corner Flash", ThrowType.STANDING,
                        68.0, 75.0, 75.2, 60.4,
                        "https://www.youtube.com/watch?v=kXYiU_JCYtU",
                        "Stand at blue dumpster outside long doors, aim at wire intersection, left click throw."),

                createLineup("b0000000-0000-0000-0000-000000000015", "de_dust2", UtilityType.SMOKE, StratSide.T,
                        "Dust2: Upper Tunnels \u2192 B Doors Smoke", ThrowType.STANDING,
                        22.0, 45.0, 20.1, 28.3,
                        "https://www.youtube.com/watch?v=C0DPdy98e4c",
                        "Wedge into back right corner of upper tunnels, align with roof beam, throw."),

                createLineup("b0000000-0000-0000-0000-000000000016", "de_dust2", UtilityType.MOLOTOV, StratSide.CT,
                        "Dust2: Mid Doors \u2192 Short Molotov", ThrowType.WALK_JUMPTHROW,
                        48.0, 50.0, 52.0, 42.0,
                        "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
                        "Take one step forward from CT mid double doors, jumpthrow towards catwalk stairs."),

                createLineup("b0000000-0000-0000-0000-000000000017", "de_dust2", UtilityType.HE, StratSide.BOTH,
                        "Dust2: Long Doors \u2192 Pit HE Grenade", ThrowType.RUNNING,
                        69.0, 65.0, 78.0, 48.0,
                        "https://www.youtube.com/watch?v=OPf0YbXqDm0",
                        "Running throw through long doors straight into pit to dislodge AWP camper."),

                createLineup("b0000000-0000-0000-0000-000000000018", "de_dust2", UtilityType.FLASH, StratSide.CT,
                        "Dust2: CT Mid \u2192 Tunnels Exit Flash", ThrowType.CROUCH,
                        42.0, 48.0, 28.0, 40.0,
                        "https://www.youtube.com/watch?v=V-_O7nl0Ii0",
                        "Crouch by mid B doors and throw underhand flash to blind upper tunnel rushers."),

                // Nuke (de_nuke)
                createLineup("b0000000-0000-0000-0000-000000000019", "de_nuke", UtilityType.SMOKE, StratSide.T,
                        "Nuke: Silo \u2192 Garage Cross Smoke", ThrowType.JUMPTHROW,
                        35.5, 80.0, 38.2, 58.0,
                        "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
                        "Align on Silo catwalk, aim at crane tip, jumpthrow for outside cross smoke #1."),

                createLineup("b0000000-0000-0000-0000-000000000020", "de_nuke", UtilityType.SMOKE, StratSide.T,
                        "Nuke: T Roof \u2192 Secret Cross Smoke", ThrowType.JUMPTHROW,
                        36.5, 80.0, 39.1, 50.0,
                        "https://www.youtube.com/watch?v=SarcasticHyperOtter",
                        "Second part of outside smoke wall, blocks Secret sightline from CT main."),

                createLineup("b0000000-0000-0000-0000-000000000021", "de_nuke", UtilityType.MOLOTOV, StratSide.T,
                        "Nuke: T Roof \u2192 Hut Molotov", ThrowType.STANDING,
                        44.0, 72.0, 48.0, 56.0,
                        "https://www.youtube.com/watch?v=60ItHLz5WEA",
                        "Stand by roof ventilator, aim at skybox antenna, left-click throw to burn inside Hut."),

                createLineup("b0000000-0000-0000-0000-000000000022", "de_nuke", UtilityType.FLASH, StratSide.T,
                        "Nuke: Lobby \u2192 A Site Pop Flash", ThrowType.RUNNING,
                        46.0, 68.0, 50.0, 52.0,
                        "https://www.youtube.com/watch?v=BlushingHedonisticSparrow",
                        "Running flash through squeaky door ceiling grating, perfectly pops above silo."),

                createLineup("b0000000-0000-0000-0000-000000000023", "de_nuke", UtilityType.HE, StratSide.CT,
                        "Nuke: Outside Garage \u2192 Secret Retake HE", ThrowType.WALK_JUMPTHROW,
                        40.0, 54.0, 38.0, 44.0,
                        "https://www.youtube.com/watch?v=oHg5SJYRHA0",
                        "Walk jumpthrow over red container into secret stairs to finish low HP bomb planters."),

                createLineup("b0000000-0000-0000-0000-000000000024", "de_nuke", UtilityType.FLASH, StratSide.BOTH,
                        "Nuke: Heaven \u2192 Ramp Flash", ThrowType.CROUCH,
                        58.0, 48.0, 64.0, 55.0,
                        "https://www.youtube.com/watch?v=uelHwf8o7_U",
                        "Crouch by heaven catwalk railing, drop flash directly into ramp room entrance."),

                // Ancient (de_ancient)
                createLineup("b0000000-0000-0000-0000-000000000025", "de_ancient", UtilityType.SMOKE, StratSide.T,
                        "Ancient: T Spawn \u2192 Red Room Smoke", ThrowType.JUMPTHROW,
                        48.0, 72.0, 52.0, 48.0,
                        "https://www.youtube.com/watch?v=eBGIQ7ZuuiU",
                        "Stand by T spawn ruins, aim at top foliage vertex, jumpthrow to block Red Room sniper."),

                createLineup("b0000000-0000-0000-0000-000000000026", "de_ancient", UtilityType.SMOKE, StratSide.T,
                        "Ancient: Mid Yard \u2192 Donut Smoke", ThrowType.STANDING,
                        50.0, 68.0, 56.0, 52.0,
                        "https://www.youtube.com/watch?v=OPf0YbXqDm0",
                        "Align against middle pillar, aim at carving eye on wall, standard standing throw."),

                createLineup("b0000000-0000-0000-0000-000000000027", "de_ancient", UtilityType.MOLOTOV, StratSide.T,
                        "Ancient: B Ramp \u2192 Cave Molotov", ThrowType.RUNNING,
                        30.0, 65.0, 32.0, 42.0,
                        "https://www.youtube.com/watch?v=kffacxfA7G4",
                        "Running throw down B ramp that spreads across cave entrance and default box."),

                createLineup("b0000000-0000-0000-0000-000000000028", "de_ancient", UtilityType.FLASH, StratSide.T,
                        "Ancient: A Main \u2192 Temple Entrance Flash", ThrowType.JUMPTHROW,
                        62.0, 66.0, 68.0, 52.0,
                        "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
                        "High popflash over the temple archway blinding all CTs defending A site."),

                createLineup("b0000000-0000-0000-0000-000000000029", "de_ancient", UtilityType.HE, StratSide.CT,
                        "Ancient: CT Spawn \u2192 Mid Donut HE Grenade", ThrowType.WALK_JUMPTHROW,
                        52.0, 36.0, 51.0, 46.0,
                        "https://www.youtube.com/watch?v=2Vv-BfVoq4g",
                        "Walk jumpthrow through CT connector to explode against mid aggression."),

                createLineup("b0000000-0000-0000-0000-000000000030", "de_ancient", UtilityType.SMOKE, StratSide.BOTH,
                        "Ancient: Cheetah \u2192 B Site Retake Smoke", ThrowType.CROUCH,
                        36.0, 50.0, 30.0, 38.0,
                        "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
                        "Crouch behind cheetah pillar, aim at top left moss, stand and throw to extinguish site mollies."),

                // Anubis (de_anubis)
                createLineup("b0000000-0000-0000-0000-000000000031", "de_anubis", UtilityType.SMOKE, StratSide.T,
                        "Anubis: B Main \u2192 B Connector Smoke", ThrowType.JUMPTHROW,
                        35.0, 70.0, 28.0, 52.0,
                        "https://www.youtube.com/watch?v=L_LUpnjgPso",
                        "Align with palace column in B Main, aim at tower top ornament, jumpthrow."),

                createLineup("b0000000-0000-0000-0000-000000000032", "de_anubis", UtilityType.SMOKE, StratSide.T,
                        "Anubis: Canal Bridge \u2192 Mid Doors Smoke", ThrowType.STANDING,
                        50.0, 75.0, 48.0, 52.0,
                        "https://www.youtube.com/watch?v=hT_nvWreIhg",
                        "Stand on wooden bridge, aim at the arch corner, standard throw to blind sniper nest."),

                createLineup("b0000000-0000-0000-0000-000000000033", "de_anubis", UtilityType.MOLOTOV, StratSide.T,
                        "Anubis: A Main \u2192 Heaven Molotov", ThrowType.WALK_JUMPTHROW,
                        66.0, 72.0, 72.0, 54.0,
                        "https://www.youtube.com/watch?v=HonestGlamorousWaffle",
                        "Walk one step and jumpthrow through A Main opening to clear Heaven perch."),

                createLineup("b0000000-0000-0000-0000-000000000034", "de_anubis", UtilityType.FLASH, StratSide.T,
                        "Anubis: Canal \u2192 B Site Pop Flash", ThrowType.RUNNING,
                        38.0, 68.0, 32.0, 58.0,
                        "https://www.youtube.com/watch?v=kJQP7kiw5F0",
                        "Run throw across water over the canal roof, blinding site pillars and alleyway."),

                createLineup("b0000000-0000-0000-0000-000000000035", "de_anubis", UtilityType.HE, StratSide.CT,
                        "Anubis: CT Spawn \u2192 Mid Canal HE Grenade", ThrowType.JUMPTHROW,
                        52.0, 38.0, 49.0, 58.0,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        "Jumpthrow from CT spawn stairs to blast water pushers before they round connector."),

                createLineup("b0000000-0000-0000-0000-000000000036", "de_anubis", UtilityType.SMOKE, StratSide.BOTH,
                        "Anubis: B Alley \u2192 Default Plant Site Smoke", ThrowType.CROUCH,
                        26.0, 48.0, 28.0, 40.0,
                        "https://www.youtube.com/watch?v=V-_O7nl0Ii0",
                        "Crouch under awning, aim at center hieroglyph, release for safe B bomb plant.")
        );
    }

    private UtilityLineupEntity createLineup(
            String idStr, String mapName, UtilityType utilityType, StratSide teamSide, String title,
            ThrowType throwType, Double standX, Double standY, Double landX, Double landY,
            String videoUrl, String description
    ) {
        return UtilityLineupEntity.builder()
                .id(UUID.fromString(idStr))
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
                .createdByUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .isVerified(true)
                .createdAt(Instant.now())
                .build();
    }
}
