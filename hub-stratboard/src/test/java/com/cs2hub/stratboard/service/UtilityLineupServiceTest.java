package com.cs2hub.stratboard.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.ThrowType;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import com.cs2hub.stratboard.dto.SubmitLineupRequestDto;
import com.cs2hub.stratboard.dto.UtilityLineupDto;
import com.cs2hub.stratboard.repository.UtilityLineupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilityLineupServiceTest {

    @Mock
    private UtilityLineupRepository utilityLineupRepository;

    private UtilityLineupService service;

    @BeforeEach
    void setUp() {
        service = new UtilityLineupService(utilityLineupRepository);
    }

    @Test
    @DisplayName("Startup seeder should populate curated lineups when database is empty")
    void shouldSeedLineupsWhenEmpty() {
        when(utilityLineupRepository.count()).thenReturn(0L);

        service.onStartup();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UtilityLineupEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(utilityLineupRepository).saveAll(captor.capture());

        List<UtilityLineupEntity> savedList = captor.getValue();
        assertThat(savedList).hasSizeGreaterThanOrEqualTo(16);
        assertThat(savedList).allMatch(UtilityLineupEntity::getIsVerified);
    }

    @Test
    @DisplayName("Startup seeder should skip seeding when lineups already exist")
    void shouldSkipSeedingWhenNotEmpty() {
        when(utilityLineupRepository.count()).thenReturn(18L);

        service.onStartup();

        verify(utilityLineupRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getLineups should query repository with specifications and return DTOs")
    void shouldGetLineups() {
        UtilityLineupEntity entity = UtilityLineupEntity.builder()
                .id(UUID.randomUUID())
                .mapName("de_mirage")
                .utilityType(UtilityType.SMOKE)
                .teamSide(StratSide.T)
                .title("Window Smoke")
                .throwType(ThrowType.JUMP_THROW)
                .standX(48.2)
                .standY(18.5)
                .landX(44.0)
                .landY(46.0)
                .videoUrl("https://sample.video")
                .description("Aim at trash can corner")
                .isVerified(true)
                .createdAt(Instant.now())
                .build();

        when(utilityLineupRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(entity));

        List<UtilityLineupDto> results = service.getLineups("de_mirage", UtilityType.SMOKE, StratSide.T);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Window Smoke");
        assertThat(results.get(0).mapName()).isEqualTo("de_mirage");
    }

    @Test
    @DisplayName("getLineupById should return DTO when found or throw 404 when missing")
    void shouldGetLineupByIdOrThrow() {
        UUID lineupId = UUID.randomUUID();
        UtilityLineupEntity entity = UtilityLineupEntity.builder()
                .id(lineupId)
                .mapName("de_inferno")
                .utilityType(UtilityType.MOLOTOV)
                .teamSide(StratSide.T)
                .title("Banana Molotov")
                .throwType(ThrowType.RUN_THROW)
                .standX(30.0)
                .standY(40.0)
                .landX(35.0)
                .landY(50.0)
                .videoUrl("https://sample.video")
                .isVerified(true)
                .createdAt(Instant.now())
                .build();

        when(utilityLineupRepository.findById(lineupId)).thenReturn(Optional.of(entity));

        UtilityLineupDto found = service.getLineupById(lineupId);
        assertThat(found).isNotNull();
        assertThat(found.title()).isEqualTo("Banana Molotov");

        UUID unknownId = UUID.randomUUID();
        when(utilityLineupRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLineupById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utility lineup not found");
    }

    @Test
    @DisplayName("submitLineup should save user-submitted lineup with isVerified false")
    void shouldSubmitLineupSuccessfully() {
        UUID userId = UUID.randomUUID();
        SubmitLineupRequestDto request = new SubmitLineupRequestDto(
                "de_dust2",
                UtilityType.SMOKE,
                StratSide.T,
                "Xbox Smoke",
                ThrowType.JUMP_THROW,
                20.5,
                30.5,
                45.0,
                55.0,
                "https://youtu.be/sample",
                "Aim at the notch"
        );

        when(utilityLineupRepository.save(any(UtilityLineupEntity.class))).thenAnswer(invocation -> {
            UtilityLineupEntity argument = invocation.getArgument(0);
            return UtilityLineupEntity.builder()
                    .id(UUID.randomUUID())
                    .createdByUserId(argument.getCreatedByUserId())
                    .mapName(argument.getMapName())
                    .utilityType(argument.getUtilityType())
                    .teamSide(argument.getTeamSide())
                    .title(argument.getTitle())
                    .throwType(argument.getThrowType())
                    .standX(argument.getStandX())
                    .standY(argument.getStandY())
                    .landX(argument.getLandX())
                    .landY(argument.getLandY())
                    .videoUrl(argument.getVideoUrl())
                    .description(argument.getDescription())
                    .isVerified(false)
                    .createdAt(Instant.now())
                    .build();
        });

        UtilityLineupDto submitted = service.submitLineup(userId, request);

        assertThat(submitted).isNotNull();
        assertThat(submitted.isVerified()).isFalse();
        assertThat(submitted.createdByUserId()).isEqualTo(userId);
        assertThat(submitted.title()).isEqualTo("Xbox Smoke");
    }

    @Test
    @DisplayName("submitLineup should reject invalid map name or out-of-bounds coordinates")
    void shouldRejectInvalidSubmissions() {
        UUID userId = UUID.randomUUID();

        // Invalid map
        SubmitLineupRequestDto badMapRequest = new SubmitLineupRequestDto(
                "de_invalid_map",
                UtilityType.SMOKE,
                StratSide.T,
                "Bad Map Smoke",
                ThrowType.STANDING,
                20.0, 20.0, 30.0, 30.0,
                "https://youtube.com/clip",
                null
        );

        assertThatThrownBy(() -> service.submitLineup(userId, badMapRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported CS2 map");

        // Out of bounds coordinate
        SubmitLineupRequestDto badCoordRequest = new SubmitLineupRequestDto(
                "de_mirage",
                UtilityType.SMOKE,
                StratSide.T,
                "Out of bounds Smoke",
                ThrowType.STANDING,
                120.0, 20.0, 30.0, 30.0,
                "https://youtube.com/clip",
                null
        );

        assertThatThrownBy(() -> service.submitLineup(userId, badCoordRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Radar coordinate");
    }

    @Test
    @DisplayName("verifyLineup should update verified status")
    void shouldVerifyLineup() {
        UUID lineupId = UUID.randomUUID();
        UtilityLineupEntity entity = UtilityLineupEntity.builder()
                .id(lineupId)
                .mapName("de_mirage")
                .utilityType(UtilityType.FLASH)
                .teamSide(StratSide.CT)
                .title("A Ramp Popflash")
                .throwType(ThrowType.STANDING)
                .standX(50.0)
                .standY(50.0)
                .landX(52.0)
                .landY(52.0)
                .videoUrl("https://youtube.com/clip")
                .isVerified(false)
                .createdAt(Instant.now())
                .build();

        when(utilityLineupRepository.findById(lineupId)).thenReturn(Optional.of(entity));
        when(utilityLineupRepository.save(any(UtilityLineupEntity.class))).thenAnswer(i -> i.getArgument(0));

        UtilityLineupDto verifiedDto = service.verifyLineup(lineupId);

        assertThat(verifiedDto.isVerified()).isTrue();
    }
}
