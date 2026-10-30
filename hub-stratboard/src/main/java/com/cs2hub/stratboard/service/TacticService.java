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
import com.cs2hub.stratboard.repository.TacticSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
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
public class TacticService {

    private final TacticRepository tacticRepository;
    private final UserLookupService userLookupService;

    private static final String SHARE_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom secureRandom = new SecureRandom();

    public TacticDetailDto createTactic(UUID userId, SaveTacticRequestDto request) {
        if (!StringUtils.hasText(request.canvasStateJson())) {
            throw new BadRequestException("Canvas state JSON is required");
        }

        String shareCode = generateUniqueShareCode();

        TacticEntity entity = TacticEntity.builder()
                .authorUserId(userId)
                .title(request.title().trim())
                .mapName(request.mapName().trim().toLowerCase())
                .teamSide(request.teamSide())
                .description(request.description())
                .canvasStateJson(request.canvasStateJson())
                .isPublic(request.isPublic() == null || request.isPublic())
                .upvotes(0)
                .shareCode(shareCode)
                .build();

        TacticEntity saved = tacticRepository.save(entity);
        log.info("Saved new tactic '{}' ({}) by author {}", saved.getTitle(), saved.getShareCode(), userId);

        Optional<UserSummaryDto> author = userLookupService.findById(userId);
        return mapToDetailDto(saved, author.orElse(null));
    }

    public TacticDetailDto updateTactic(UUID tacticId, AuthenticatedUser currentUser, SaveTacticRequestDto request) {
        TacticEntity entity = tacticRepository.findById(tacticId)
                .orElseThrow(() -> new ResourceNotFoundException("Tactic not found with ID: " + tacticId));

        boolean isOwner = entity.getAuthorUserId().equals(currentUser.userId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to edit this tactic");
        }

        entity.setTitle(request.title().trim());
        entity.setMapName(request.mapName().trim().toLowerCase());
        entity.setTeamSide(request.teamSide());
        entity.setDescription(request.description());
        entity.setCanvasStateJson(request.canvasStateJson());
        if (request.isPublic() != null) {
            entity.setIsPublic(request.isPublic());
        }

        TacticEntity saved = tacticRepository.save(entity);
        log.info("Updated tactic {} by user {}", tacticId, currentUser.userId());

        Optional<UserSummaryDto> author = userLookupService.findById(saved.getAuthorUserId());
        return mapToDetailDto(saved, author.orElse(null));
    }

    @Transactional(readOnly = true)
    public TacticDetailDto getTacticByIdOrShareCode(String idOrShareCode, Optional<UUID> currentUserId) {
        if (!StringUtils.hasText(idOrShareCode)) {
            throw new ResourceNotFoundException("Tactic identifier is required");
        }

        String identifier = idOrShareCode.trim();
        Optional<TacticEntity> found = Optional.empty();

        // Check if string is a valid UUID
        try {
            UUID uuid = UUID.fromString(identifier);
            found = tacticRepository.findById(uuid);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, lookup by shareCode
        }

        if (found.isEmpty()) {
            found = tacticRepository.findByShareCode(identifier.toUpperCase());
        }

        TacticEntity tactic = found.orElseThrow(() ->
                new ResourceNotFoundException("Tactic not found for identifier: " + identifier));

        if (!Boolean.TRUE.equals(tactic.getIsPublic())) {
            boolean isOwner = currentUserId.map(uid -> uid.equals(tactic.getAuthorUserId())).orElse(false);
            if (!isOwner) {
                throw new ResourceNotFoundException("Tactic not found or is private");
            }
        }

        Optional<UserSummaryDto> author = userLookupService.findById(tactic.getAuthorUserId());
        return mapToDetailDto(tactic, author.orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<TacticSummaryDto> getPublicTactics(String mapName, StratSide teamSide, int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                validatedPage,
                validatedSize,
                Sort.by(Sort.Direction.DESC, "upvotes").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<TacticEntity> pageResult = tacticRepository.findAll(
                TacticSpecifications.filterPublic(mapName, teamSide),
                pageable
        );

        if (pageResult.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> authorIds = pageResult.getContent().stream()
                .map(TacticEntity::getAuthorUserId)
                .collect(Collectors.toSet());

        Map<UUID, UserSummaryDto> authorsMap = userLookupService.findByIds(authorIds);

        return pageResult.map(tactic -> mapToSummaryDto(tactic, authorsMap.get(tactic.getAuthorUserId())));
    }

    @Transactional(readOnly = true)
    public List<TacticSummaryDto> getUserTactics(UUID userId) {
        List<TacticEntity> tactics = tacticRepository.findAllByAuthorUserIdOrderByCreatedAtDesc(userId);
        if (tactics.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<UserSummaryDto> author = userLookupService.findById(userId);
        UserSummaryDto authorDto = author.orElse(null);

        return tactics.stream()
                .map(t -> mapToSummaryDto(t, authorDto))
                .toList();
    }

    public void deleteTactic(UUID tacticId, AuthenticatedUser currentUser) {
        TacticEntity entity = tacticRepository.findById(tacticId)
                .orElseThrow(() -> new ResourceNotFoundException("Tactic not found with ID: " + tacticId));

        boolean isOwner = entity.getAuthorUserId().equals(currentUser.userId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this tactic");
        }

        tacticRepository.delete(entity);
        log.info("Deleted tactic {} by user {}", tacticId, currentUser.userId());
    }

    private String generateUniqueShareCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(8);
            for (int j = 0; j < 8; j++) {
                sb.append(SHARE_CODE_CHARS.charAt(secureRandom.nextInt(SHARE_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!tacticRepository.existsByShareCode(code)) {
                return code;
            }
        }
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TacticSummaryDto mapToSummaryDto(TacticEntity entity, UserSummaryDto author) {
        return new TacticSummaryDto(
                entity.getId(),
                entity.getAuthorUserId(),
                entity.getTitle(),
                entity.getMapName(),
                entity.getTeamSide(),
                entity.getDescription(),
                entity.getIsPublic(),
                entity.getUpvotes(),
                entity.getShareCode(),
                author,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TacticDetailDto mapToDetailDto(TacticEntity entity, UserSummaryDto author) {
        return new TacticDetailDto(
                entity.getId(),
                entity.getAuthorUserId(),
                entity.getTitle(),
                entity.getMapName(),
                entity.getTeamSide(),
                entity.getDescription(),
                entity.getCanvasStateJson(),
                entity.getIsPublic(),
                entity.getUpvotes(),
                entity.getShareCode(),
                author,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
