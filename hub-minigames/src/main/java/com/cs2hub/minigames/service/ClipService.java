package com.cs2hub.minigames.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.VideoProvider;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.ReviewClipRequestDto;
import com.cs2hub.minigames.dto.SubmitClipRequestDto;
import com.cs2hub.minigames.repository.ClipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClipService {

    private final ClipRepository clipRepository;
    private final UserLookupService userLookupService;

    @Transactional
    public ClipSubmissionResponseDto submitClip(AuthenticatedUser user, SubmitClipRequestDto request) {
        VideoProvider provider = VideoProvider.fromUrl(request.videoUrl());

        ClipEntity clip = ClipEntity.builder()
                .id(UUID.randomUUID())
                .submittedByUserId(user.userId())
                .title(request.title().trim())
                .videoUrl(request.videoUrl().trim())
                .videoProvider(provider)
                .mapName(request.mapName().trim().toLowerCase())
                .actualPremierRating(request.actualPremierRating())
                .actualFaceitLevel(request.actualFaceitLevel())
                .status(ClipStatus.PENDING)
                .build();

        ClipEntity saved = clipRepository.save(clip);

        UserSummaryDto submitter = userLookupService.findById(user.userId())
                .orElse(new UserSummaryDto(user.userId(), user.steamId(), user.username(), null));

        return toDto(saved, submitter);
    }

    @Transactional(readOnly = true)
    public List<ClipSubmissionResponseDto> getMyClips(AuthenticatedUser user) {
        List<ClipEntity> clips = clipRepository.findAllBySubmittedByUserIdOrderByCreatedAtDesc(user.userId());
        if (clips.isEmpty()) {
            return Collections.emptyList();
        }

        UserSummaryDto submitter = userLookupService.findById(user.userId())
                .orElse(new UserSummaryDto(user.userId(), user.steamId(), user.username(), null));

        return clips.stream()
                .map(clip -> toDto(clip, submitter))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClipSubmissionResponseDto> getClipsForModeration(ClipStatus status) {
        ClipStatus targetStatus = status != null ? status : ClipStatus.PENDING;
        List<ClipEntity> clips = clipRepository.findAllByStatusOrderByCreatedAtDesc(targetStatus);
        if (clips.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> submitterIds = clips.stream()
                .map(ClipEntity::getSubmittedByUserId)
                .collect(Collectors.toSet());

        Map<UUID, UserSummaryDto> userMap = userLookupService.findByIds(submitterIds);

        return clips.stream()
                .map(clip -> toDto(clip, userMap.get(clip.getSubmittedByUserId())))
                .toList();
    }

    @Transactional
    public ClipSubmissionResponseDto reviewClip(UUID clipId, AuthenticatedUser adminUser, ReviewClipRequestDto request) {
        if (request.status() != ClipStatus.APPROVED && request.status() != ClipStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED");
        }

        ClipEntity clip = clipRepository.findById(clipId)
                .orElseThrow(() -> new ResourceNotFoundException("Clip not found with ID: " + clipId));

        clip.setStatus(request.status());
        clip.setReviewedByUserId(adminUser.userId());

        if (request.overridePremierRating() != null) {
            clip.setActualPremierRating(request.overridePremierRating());
        }

        ClipEntity updated = clipRepository.save(clip);

        UserSummaryDto submitter = userLookupService.findById(updated.getSubmittedByUserId())
                .orElse(null);

        log.info("Clip {} reviewed by admin {}: status={}", clipId, adminUser.userId(), request.status());

        return toDto(updated, submitter);
    }

    private ClipSubmissionResponseDto toDto(ClipEntity entity, UserSummaryDto submitter) {
        return new ClipSubmissionResponseDto(
                entity.getId(),
                entity.getTitle(),
                entity.getVideoUrl(),
                entity.getVideoProvider(),
                entity.getMapName(),
                entity.getActualPremierRating(),
                entity.getActualFaceitLevel(),
                entity.getStatus(),
                submitter,
                entity.getReviewedByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
