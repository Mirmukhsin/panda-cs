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
import com.cs2hub.loadouts.dto.SaveLoadoutItemDto;
import com.cs2hub.loadouts.dto.SaveLoadoutRequestDto;
import com.cs2hub.loadouts.dto.StickerPlacementDto;
import com.cs2hub.loadouts.dto.UpvoteResponseDto;
import com.cs2hub.loadouts.dto.UserLoadoutDetailDto;
import com.cs2hub.loadouts.dto.UserLoadoutItemDetailDto;
import com.cs2hub.loadouts.dto.UserLoadoutSummaryDto;
import com.cs2hub.loadouts.repository.LoadoutUpvoteRepository;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.StickerRepository;
import com.cs2hub.loadouts.repository.UserLoadoutItemRepository;
import com.cs2hub.loadouts.repository.UserLoadoutRepository;
import com.cs2hub.loadouts.repository.UserLoadoutSpecifications;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityLoadoutService {

    private final UserLoadoutRepository userLoadoutRepository;
    private final UserLoadoutItemRepository userLoadoutItemRepository;
    private final LoadoutUpvoteRepository loadoutUpvoteRepository;
    private final SkinRepository skinRepository;
    private final StickerRepository stickerRepository;
    private final UserLookupService userLookupService;
    private final ObjectMapper objectMapper;

    public UserLoadoutDetailDto createLoadout(UUID userId, SaveLoadoutRequestDto request) {
        log.info("Creating user loadout '{}' for userId {}", request.title(), userId);

        UserLoadoutEntity loadout = UserLoadoutEntity.builder()
                .userId(userId)
                .title(request.title().trim())
                .themeColor(request.themeColor())
                .budgetLimitUsd(request.budgetLimitUsd())
                .isPublic(request.isPublic() == null || request.isPublic())
                .upvotes(0)
                .totalCostUsd(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal runningTotal = BigDecimal.ZERO;
        List<UserLoadoutItemEntity> items = new ArrayList<>();

        for (SaveLoadoutItemDto itemReq : request.items()) {
            SkinEntity skin = skinRepository.findById(itemReq.skinId())
                    .orElseThrow(() -> new BadRequestException("Skin not found with ID: " + itemReq.skinId()));

            BigDecimal itemCost = skin.getPriceForCondition(itemReq.wearCondition());

            String stickersJson = null;
            if (itemReq.stickers() != null && !itemReq.stickers().isEmpty()) {
                if (itemReq.stickers().size() > 5) {
                    throw new BadRequestException("A weapon can hold a maximum of 5 stickers");
                }
                for (StickerPlacementDto sp : itemReq.stickers()) {
                    StickerEntity sticker = stickerRepository.findById(sp.stickerId())
                            .orElseThrow(() -> new BadRequestException("Sticker not found with ID: " + sp.stickerId()));
                    if (sticker.getPriceUsd() != null) {
                        itemCost = itemCost.add(sticker.getPriceUsd());
                    }
                }
                try {
                    stickersJson = objectMapper.writeValueAsString(itemReq.stickers());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid sticker placement format");
                }
            }

            runningTotal = runningTotal.add(itemCost);

            UserLoadoutItemEntity itemEntity = UserLoadoutItemEntity.builder()
                    .loadout(loadout)
                    .skin(skin)
                    .weaponSlot(itemReq.weaponSlot())
                    .wearCondition(itemReq.wearCondition())
                    .selectedPriceUsd(itemCost.setScale(2, RoundingMode.HALF_UP))
                    .stickersJson(stickersJson)
                    .build();

            items.add(itemEntity);
        }

        loadout.setTotalCostUsd(runningTotal.setScale(2, RoundingMode.HALF_UP));
        loadout.setItems(items);

        UserLoadoutEntity savedLoadout = userLoadoutRepository.save(loadout);

        Optional<UserSummaryDto> author = userLookupService.findById(userId);
        return mapToDetailDto(savedLoadout, author.orElse(null), false);
    }

    @Transactional(readOnly = true)
    public Page<UserLoadoutSummaryDto> getPublicLoadouts(
            SkinColor color,
            BigDecimal maxBudget,
            String sortBy,
            int page,
            int size,
            Optional<UUID> currentUserId
    ) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, 1), 100);

        Sort sort = "latest".equalsIgnoreCase(sortBy)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "upvotes").and(Sort.by(Sort.Direction.DESC, "createdAt"));

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sort);

        Page<UserLoadoutEntity> pageResult = userLoadoutRepository.findAll(
                UserLoadoutSpecifications.filterPublic(color, maxBudget),
                pageable
        );

        if (pageResult.isEmpty()) {
            return Page.empty(pageable);
        }

        // Batch resolve author profiles via SPI
        Set<UUID> authorIds = pageResult.getContent().stream()
                .map(UserLoadoutEntity::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryDto> authorsMap = userLookupService.findByIds(authorIds);

        // Batch resolve current user upvotes
        Set<UUID> upvotedLoadoutIds = currentUserId
                .map(uid -> loadoutUpvoteRepository.findUpvotedLoadoutIds(
                        uid,
                        pageResult.getContent().stream().map(UserLoadoutEntity::getId).toList()))
                .orElse(Collections.emptySet());

        return pageResult.map(loadout -> mapToSummaryDto(
                loadout,
                authorsMap.get(loadout.getUserId()),
                upvotedLoadoutIds.contains(loadout.getId())
        ));
    }

    @Transactional(readOnly = true)
    public UserLoadoutDetailDto getLoadoutById(UUID loadoutId, Optional<UUID> currentUserId) {
        UserLoadoutEntity loadout = userLoadoutRepository.findById(loadoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Loadout not found with ID: " + loadoutId));

        if (!Boolean.TRUE.equals(loadout.getIsPublic())) {
            boolean isOwner = currentUserId.map(uid -> uid.equals(loadout.getUserId())).orElse(false);
            if (!isOwner) {
                throw new ResourceNotFoundException("Loadout not found or is private");
            }
        }

        boolean upvoted = currentUserId
                .map(uid -> loadoutUpvoteRepository.existsByLoadoutIdAndUserId(loadoutId, uid))
                .orElse(false);

        Optional<UserSummaryDto> author = userLookupService.findById(loadout.getUserId());
        return mapToDetailDto(loadout, author.orElse(null), upvoted);
    }

    @Transactional(readOnly = true)
    public List<UserLoadoutSummaryDto> getUserLoadouts(UUID userId) {
        List<UserLoadoutEntity> loadouts = userLoadoutRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (loadouts.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<UserSummaryDto> author = userLookupService.findById(userId);
        UserSummaryDto authorDto = author.orElse(null);

        Set<UUID> upvotedIds = loadoutUpvoteRepository.findUpvotedLoadoutIds(
                userId,
                loadouts.stream().map(UserLoadoutEntity::getId).toList()
        );

        return loadouts.stream()
                .map(l -> mapToSummaryDto(l, authorDto, upvotedIds.contains(l.getId())))
                .toList();
    }

    public UpvoteResponseDto toggleUpvote(UUID loadoutId, UUID userId) {
        UserLoadoutEntity loadout = userLoadoutRepository.findById(loadoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Loadout not found with ID: " + loadoutId));

        boolean exists = loadoutUpvoteRepository.existsByLoadoutIdAndUserId(loadoutId, userId);
        boolean nowUpvoted;

        if (exists) {
            loadoutUpvoteRepository.deleteByLoadoutIdAndUserId(loadoutId, userId);
            loadout.setUpvotes(Math.max(0, loadout.getUpvotes() - 1));
            nowUpvoted = false;
        } else {
            LoadoutUpvoteEntity upvote = LoadoutUpvoteEntity.builder()
                    .loadoutId(loadoutId)
                    .userId(userId)
                    .build();
            loadoutUpvoteRepository.save(upvote);
            loadout.setUpvotes(loadout.getUpvotes() + 1);
            nowUpvoted = true;
        }

        userLoadoutRepository.save(loadout);
        return new UpvoteResponseDto(loadoutId, loadout.getUpvotes(), nowUpvoted);
    }

    public void deleteLoadout(UUID loadoutId, AuthenticatedUser currentUser) {
        UserLoadoutEntity loadout = userLoadoutRepository.findById(loadoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Loadout not found with ID: " + loadoutId));

        boolean isOwner = loadout.getUserId().equals(currentUser.userId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this loadout");
        }

        userLoadoutRepository.delete(loadout);
        log.info("Deleted loadout {} by user {}", loadoutId, currentUser.userId());
    }

    private UserLoadoutSummaryDto mapToSummaryDto(UserLoadoutEntity entity, UserSummaryDto author, boolean upvoted) {
        return new UserLoadoutSummaryDto(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getThemeColor(),
                entity.getBudgetLimitUsd(),
                entity.getTotalCostUsd(),
                entity.getUpvotes(),
                entity.getIsPublic(),
                entity.getItems() != null ? entity.getItems().size() : 0,
                author,
                upvoted,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserLoadoutDetailDto mapToDetailDto(UserLoadoutEntity entity, UserSummaryDto author, boolean upvoted) {
        List<UserLoadoutItemDetailDto> itemDtos = entity.getItems() != null
                ? entity.getItems().stream().map(this::mapItemToDetailDto).toList()
                : Collections.emptyList();

        return new UserLoadoutDetailDto(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getThemeColor(),
                entity.getBudgetLimitUsd(),
                entity.getTotalCostUsd(),
                entity.getUpvotes(),
                entity.getIsPublic(),
                itemDtos,
                author,
                upvoted,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserLoadoutItemDetailDto mapItemToDetailDto(UserLoadoutItemEntity item) {
        List<StickerPlacementDto> stickers = Collections.emptyList();
        if (StringUtils.hasText(item.getStickersJson())) {
            try {
                stickers = objectMapper.readValue(item.getStickersJson(), new TypeReference<List<StickerPlacementDto>>() {});
            } catch (Exception e) {
                log.warn("Failed deserializing stickersJson for item {}: {}", item.getId(), e.getMessage());
            }
        }

        SkinEntity skin = item.getSkin();
        return new UserLoadoutItemDetailDto(
                item.getId(),
                item.getWeaponSlot(),
                skin.getId(),
                skin.getName(),
                skin.getWeaponName(),
                item.getWearCondition(),
                item.getSelectedPriceUsd(),
                skin.getPrimaryColor(),
                skin.getImageUrl(),
                stickers
        );
    }
}
