package com.cs2hub.stratboard.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.TacticEntity;
import com.cs2hub.stratboard.dto.SaveTacticRequestDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.cs2hub.stratboard.dto.TacticSummaryDto;
import com.cs2hub.stratboard.repository.TacticRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacticServiceTest {

    @Mock
    private TacticRepository tacticRepository;

    @Mock
    private UserLookupService userLookupService;

    private TacticService service;

    @BeforeEach
    void setUp() {
        service = new TacticService(tacticRepository, userLookupService);
    }

    @Test
    @DisplayName("createTactic should generate shareCode and save tactic")
    void shouldCreateTactic() {
        UUID userId = UUID.randomUUID();
        SaveTacticRequestDto request = new SaveTacticRequestDto(
                "A Site Split",
                "de_mirage",
                StratSide.T,
                "Execute with 3 palace 2 ramp",
                "{\"shapes\":[{\"type\":\"line\"}]}",
                true
        );

        when(tacticRepository.existsByShareCode(anyString())).thenReturn(false);
        when(tacticRepository.save(any(TacticEntity.class))).thenAnswer(i -> {
            TacticEntity e = i.getArgument(0);
            return TacticEntity.builder()
                    .id(UUID.randomUUID())
                    .authorUserId(e.getAuthorUserId())
                    .title(e.getTitle())
                    .mapName(e.getMapName())
                    .teamSide(e.getTeamSide())
                    .description(e.getDescription())
                    .canvasStateJson(e.getCanvasStateJson())
                    .isPublic(e.getIsPublic())
                    .upvotes(0)
                    .shareCode(e.getShareCode())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        UserSummaryDto authorSummary = new UserSummaryDto(userId, "76561198000000001", "Tactician", "https://avatar.url");
        when(userLookupService.findById(userId)).thenReturn(Optional.of(authorSummary));

        TacticDetailDto created = service.createTactic(userId, request);

        assertThat(created).isNotNull();
        assertThat(created.title()).isEqualTo("A Site Split");
        assertThat(created.shareCode()).hasSize(8);
        assertThat(created.author()).isNotNull();
        assertThat(created.author().username()).isEqualTo("Tactician");
    }

    @Test
    @DisplayName("createTactic should reject empty canvasStateJson")
    void shouldRejectEmptyCanvas() {
        UUID userId = UUID.randomUUID();
        SaveTacticRequestDto request = new SaveTacticRequestDto(
                "Empty Canvas Strat",
                "de_mirage",
                StratSide.T,
                "description",
                "   ",
                true
        );

        assertThatThrownBy(() -> service.createTactic(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Canvas state JSON is required");
    }

    @Test
    @DisplayName("updateTactic should allow author to update and deny unauthorized user")
    void shouldAuthorizeTacticUpdate() {
        UUID tacticId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        TacticEntity existing = TacticEntity.builder()
                .id(tacticId)
                .authorUserId(authorId)
                .title("Original Strat")
                .mapName("de_mirage")
                .teamSide(StratSide.T)
                .canvasStateJson("{}")
                .isPublic(true)
                .upvotes(5)
                .shareCode("ABCDEF12")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tacticRepository.findById(tacticId)).thenReturn(Optional.of(existing));

        SaveTacticRequestDto updateReq = new SaveTacticRequestDto(
                "Updated Strat",
                "de_mirage",
                StratSide.T,
                "New description",
                "{\"shapes\":[]}",
                false
        );

        // Deny non-author non-admin
        AuthenticatedUser stranger = new AuthenticatedUser(otherUserId, "76561198000000002", "Stranger", AuthenticatedUser.ROLE_USER);
        assertThatThrownBy(() -> service.updateTactic(tacticId, stranger, updateReq))
                .isInstanceOf(UnauthorizedException.class);

        // Allow author
        AuthenticatedUser author = new AuthenticatedUser(authorId, "76561198000000001", "Author", AuthenticatedUser.ROLE_USER);
        when(tacticRepository.save(any(TacticEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(userLookupService.findById(authorId)).thenReturn(Optional.empty());

        TacticDetailDto updated = service.updateTactic(tacticId, author, updateReq);
        assertThat(updated.title()).isEqualTo("Updated Strat");
        assertThat(updated.isPublic()).isFalse();
    }

    @Test
    @DisplayName("getTacticByIdOrShareCode should resolve both UUID and 8-char shareCode")
    void shouldLookupByUuidOrShareCode() {
        UUID tacticId = UUID.randomUUID();
        TacticEntity tactic = TacticEntity.builder()
                .id(tacticId)
                .authorUserId(UUID.randomUUID())
                .title("B Rush")
                .mapName("de_dust2")
                .teamSide(StratSide.T)
                .canvasStateJson("{\"canvas\":\"data\"}")
                .isPublic(true)
                .upvotes(10)
                .shareCode("BRUSH123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tacticRepository.findById(tacticId)).thenReturn(Optional.of(tactic));
        when(tacticRepository.findByShareCode("BRUSH123")).thenReturn(Optional.of(tactic));

        // Lookup by UUID
        TacticDetailDto byUuid = service.getTacticByIdOrShareCode(tacticId.toString(), Optional.empty());
        assertThat(byUuid.title()).isEqualTo("B Rush");

        // Lookup by share code (case-insensitive)
        TacticDetailDto byCode = service.getTacticByIdOrShareCode("brush123", Optional.empty());
        assertThat(byCode.title()).isEqualTo("B Rush");
    }

    @Test
    @DisplayName("getTacticByIdOrShareCode should protect private tactics from other users")
    void shouldProtectPrivateTactics() {
        UUID tacticId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        TacticEntity privateTactic = TacticEntity.builder()
                .id(tacticId)
                .authorUserId(authorId)
                .title("Secret Strat")
                .mapName("de_ancient")
                .teamSide(StratSide.CT)
                .canvasStateJson("{}")
                .isPublic(false)
                .upvotes(0)
                .shareCode("SECRET01")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tacticRepository.findByShareCode("SECRET01")).thenReturn(Optional.of(privateTactic));

        // Anonymous user -> 404
        assertThatThrownBy(() -> service.getTacticByIdOrShareCode("SECRET01", Optional.empty()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("private");

        // Stranger -> 404
        assertThatThrownBy(() -> service.getTacticByIdOrShareCode("SECRET01", Optional.of(strangerId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("private");

        // Author -> OK
        TacticDetailDto authorView = service.getTacticByIdOrShareCode("SECRET01", Optional.of(authorId));
        assertThat(authorView.title()).isEqualTo("Secret Strat");
    }

    @Test
    @DisplayName("getPublicTactics should return page with batch-resolved authors")
    void shouldGetPublicTacticsWithAuthors() {
        UUID author1 = UUID.randomUUID();
        TacticEntity tactic1 = TacticEntity.builder()
                .id(UUID.randomUUID())
                .authorUserId(author1)
                .title("Fast A")
                .mapName("de_mirage")
                .teamSide(StratSide.T)
                .isPublic(true)
                .upvotes(12)
                .shareCode("CODE1111")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Page<TacticEntity> page = new PageImpl<>(List.of(tactic1));
        when(tacticRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userLookupService.findByIds(Set.of(author1))).thenReturn(Map.of(
                author1, new UserSummaryDto(author1, "76561198000000001", "IGL", "https://avatar.url")
        ));

        Page<TacticSummaryDto> result = service.getPublicTactics("de_mirage", StratSide.T, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Fast A");
        assertThat(result.getContent().get(0).author().username()).isEqualTo("IGL");
    }

    @Test
    @DisplayName("deleteTactic should allow author or admin and delete entity")
    void shouldDeleteTactic() {
        UUID tacticId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        TacticEntity tactic = TacticEntity.builder()
                .id(tacticId)
                .authorUserId(authorId)
                .title("To Delete")
                .build();

        when(tacticRepository.findById(tacticId)).thenReturn(Optional.of(tactic));

        AuthenticatedUser author = new AuthenticatedUser(authorId, "76561198000000001", "Author", AuthenticatedUser.ROLE_USER);
        service.deleteTactic(tacticId, author);

        verify(tacticRepository).delete(tactic);
    }
}
