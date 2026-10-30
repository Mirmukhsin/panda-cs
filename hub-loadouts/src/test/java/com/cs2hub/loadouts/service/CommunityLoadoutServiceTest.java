package com.cs2hub.loadouts.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.loadouts.domain.LoadoutUpvoteEntity;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.StickerEntity;
import com.cs2hub.loadouts.domain.UserLoadoutEntity;
import com.cs2hub.loadouts.domain.UserLoadoutItemEntity;
import com.cs2hub.loadouts.domain.WearCondition;
import com.cs2hub.loadouts.dto.SaveLoadoutItemDto;
import com.cs2hub.loadouts.dto.SaveLoadoutRequestDto;
import com.cs2hub.loadouts.dto.StickerPlacementDto;
import com.cs2hub.loadouts.dto.UpvoteResponseDto;
import com.cs2hub.loadouts.dto.UserLoadoutDetailDto;
import com.cs2hub.loadouts.repository.LoadoutUpvoteRepository;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.StickerRepository;
import com.cs2hub.loadouts.repository.UserLoadoutItemRepository;
import com.cs2hub.loadouts.repository.UserLoadoutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityLoadoutServiceTest {

    @Mock
    private UserLoadoutRepository userLoadoutRepository;

    @Mock
    private UserLoadoutItemRepository userLoadoutItemRepository;

    @Mock
    private LoadoutUpvoteRepository loadoutUpvoteRepository;

    @Mock
    private SkinRepository skinRepository;

    @Mock
    private StickerRepository stickerRepository;

    @Mock
    private UserLookupService userLookupService;

    private ObjectMapper objectMapper;
    private CommunityLoadoutService communityLoadoutService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        communityLoadoutService = new CommunityLoadoutService(
                userLoadoutRepository,
                userLoadoutItemRepository,
                loadoutUpvoteRepository,
                skinRepository,
                stickerRepository,
                userLookupService,
                objectMapper
        );
    }

    @Test
    @DisplayName("createLoadout should calculate total cost including skin and sticker prices")
    void shouldCreateLoadoutWithStickersSuccessfully() {
        UUID userId = UUID.randomUUID();

        SkinEntity akRedline = SkinEntity.builder()
                .id("skin-ak47-redline")
                .name("AK-47 | Redline")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .primaryColor(SkinColor.RED)
                .priceFt(new BigDecimal("18.50"))
                .imageUrl("https://example.com/ak.png")
                .build();
        akRedline.recalculateLowestPrice();

        StickerEntity c9Sticker = StickerEntity.builder()
                .id("sticker-c9")
                .name("Sticker | Cloud9")
                .priceUsd(new BigDecimal("15.00"))
                .primaryColor(SkinColor.BLUE)
                .imageUrl("https://example.com/c9.png")
                .build();

        when(skinRepository.findById("skin-ak47-redline")).thenReturn(Optional.of(akRedline));
        when(stickerRepository.findById("sticker-c9")).thenReturn(Optional.of(c9Sticker));
        when(userLoadoutRepository.save(any(UserLoadoutEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveLoadoutItemDto itemReq = new SaveLoadoutItemDto(
                "T_RIFLE",
                "skin-ak47-redline",
                WearCondition.FT,
                List.of(new StickerPlacementDto("sticker-c9", 0, 0.0, 0.0, 0.0, 1.0))
        );

        SaveLoadoutRequestDto request = new SaveLoadoutRequestDto(
                "My Redline Craft",
                SkinColor.RED,
                new BigDecimal("50.00"),
                true,
                List.of(itemReq)
        );

        UserLoadoutDetailDto result = communityLoadoutService.createLoadout(userId, request);

        assertThat(result.title()).isEqualTo("My Redline Craft");
        // Cost: 18.50 (skin) + 15.00 (sticker) = 33.50
        assertThat(result.totalCostUsd()).isEqualByComparingTo("33.50");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).stickers()).hasSize(1);
    }

    @Test
    @DisplayName("createLoadout should reject if weapon item exceeds 5 stickers")
    void shouldRejectMoreThanFiveStickers() {
        UUID userId = UUID.randomUUID();

        SkinEntity akRedline = SkinEntity.builder()
                .id("skin-ak47-redline")
                .name("AK-47 | Redline")
                .weaponId("weapon_ak47")
                .weaponName("AK-47")
                .primaryColor(SkinColor.RED)
                .priceFt(new BigDecimal("18.50"))
                .build();
        akRedline.recalculateLowestPrice();

        when(skinRepository.findById("skin-ak47-redline")).thenReturn(Optional.of(akRedline));

        List<StickerPlacementDto> sixStickers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixStickers.add(new StickerPlacementDto("sticker-c9", i, 0.0, 0.0, 0.0, 1.0));
        }

        SaveLoadoutItemDto itemReq = new SaveLoadoutItemDto(
                "T_RIFLE",
                "skin-ak47-redline",
                WearCondition.FT,
                sixStickers
        );

        SaveLoadoutRequestDto request = new SaveLoadoutRequestDto(
                "Overcrafted",
                SkinColor.RED,
                new BigDecimal("50.00"),
                true,
                List.of(itemReq)
        );

        assertThatThrownBy(() -> communityLoadoutService.createLoadout(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum of 5 stickers");
    }

    @Test
    @DisplayName("toggleUpvote should insert upvote and increment count when not yet upvoted")
    void shouldAddUpvoteWhenNotYetUpvoted() {
        UUID loadoutId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserLoadoutEntity loadout = UserLoadoutEntity.builder()
                .id(loadoutId)
                .userId(UUID.randomUUID())
                .title("Cool Loadout")
                .upvotes(5)
                .isPublic(true)
                .build();

        when(userLoadoutRepository.findById(loadoutId)).thenReturn(Optional.of(loadout));
        when(loadoutUpvoteRepository.existsByLoadoutIdAndUserId(loadoutId, userId)).thenReturn(false);

        UpvoteResponseDto response = communityLoadoutService.toggleUpvote(loadoutId, userId);

        assertThat(response.upvotes()).isEqualTo(6);
        assertThat(response.upvoted()).isTrue();
        verify(loadoutUpvoteRepository).save(any(LoadoutUpvoteEntity.class));
    }

    @Test
    @DisplayName("toggleUpvote should remove upvote and decrement count when already upvoted")
    void shouldRemoveUpvoteWhenAlreadyUpvoted() {
        UUID loadoutId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserLoadoutEntity loadout = UserLoadoutEntity.builder()
                .id(loadoutId)
                .userId(UUID.randomUUID())
                .title("Cool Loadout")
                .upvotes(6)
                .isPublic(true)
                .build();

        when(userLoadoutRepository.findById(loadoutId)).thenReturn(Optional.of(loadout));
        when(loadoutUpvoteRepository.existsByLoadoutIdAndUserId(loadoutId, userId)).thenReturn(true);

        UpvoteResponseDto response = communityLoadoutService.toggleUpvote(loadoutId, userId);

        assertThat(response.upvotes()).isEqualTo(5);
        assertThat(response.upvoted()).isFalse();
        verify(loadoutUpvoteRepository).deleteByLoadoutIdAndUserId(loadoutId, userId);
    }

    @Test
    @DisplayName("deleteLoadout should succeed when owner deletes own loadout")
    void shouldAllowOwnerToDeleteLoadout() {
        UUID loadoutId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        UserLoadoutEntity loadout = UserLoadoutEntity.builder()
                .id(loadoutId)
                .userId(ownerId)
                .title("Owner's Loadout")
                .build();

        when(userLoadoutRepository.findById(loadoutId)).thenReturn(Optional.of(loadout));

        AuthenticatedUser owner = new AuthenticatedUser(ownerId, "76561198000000001", "owner", "USER");

        communityLoadoutService.deleteLoadout(loadoutId, owner);

        verify(userLoadoutRepository).delete(loadout);
    }

    @Test
    @DisplayName("deleteLoadout should throw UnauthorizedException when non-owner non-admin attempts delete")
    void shouldRejectDeleteByOtherUser() {
        UUID loadoutId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        UserLoadoutEntity loadout = UserLoadoutEntity.builder()
                .id(loadoutId)
                .userId(ownerId)
                .title("Owner's Loadout")
                .build();

        when(userLoadoutRepository.findById(loadoutId)).thenReturn(Optional.of(loadout));

        AuthenticatedUser otherUser = new AuthenticatedUser(otherId, "76561198000000002", "intruder", "USER");

        assertThatThrownBy(() -> communityLoadoutService.deleteLoadout(loadoutId, otherUser))
                .isInstanceOf(UnauthorizedException.class);
    }
}
