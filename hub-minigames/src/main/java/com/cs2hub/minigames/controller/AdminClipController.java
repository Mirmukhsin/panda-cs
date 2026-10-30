package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.minigames.domain.ClipStatus;
import com.cs2hub.minigames.domain.DailyChallengeEntity;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.DailyChallengeResponseDto;
import com.cs2hub.minigames.dto.ReviewClipRequestDto;
import com.cs2hub.minigames.service.ClipService;
import com.cs2hub.minigames.service.DailyChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Admin Minigames Moderation", description = "Administrator endpoints for reviewing community clips and generating daily challenges")
@RestController
@RequestMapping("/api/v1/minigames/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminClipController {

    private final ClipService clipService;
    private final DailyChallengeService dailyChallengeService;

    @Operation(
            summary = "List clips by moderation status",
            description = "Allows an administrator to inspect submitted clips queued for moderation.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of clips for review"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    })
    @GetMapping("/clips")
    public ResponseEntity<ApiResponseDto<List<ClipSubmissionResponseDto>>> listClipsForReview(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "Filter by clip status (default: PENDING)")
            @RequestParam(required = false, defaultValue = "PENDING") ClipStatus status
    ) {
        verifyAdmin(currentUser);
        List<ClipSubmissionResponseDto> clips = clipService.getClipsForModeration(status);
        return ResponseEntity.ok(ApiResponseDto.ok(clips));
    }

    @Operation(
            summary = "Review and moderate a clip",
            description = "Allows an administrator to approve or reject a clip, with an optional rating override.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Clip reviewed successfully",
                    content = @Content(schema = @Schema(implementation = ClipSubmissionResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid review status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required"),
            @ApiResponse(responseCode = "404", description = "Clip not found")
    })
    @PatchMapping("/clips/{clipId}/review")
    public ResponseEntity<ApiResponseDto<ClipSubmissionResponseDto>> reviewClip(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "Clip UUID")
            @PathVariable UUID clipId,
            @Valid @RequestBody ReviewClipRequestDto request
    ) {
        verifyAdmin(currentUser);
        ClipSubmissionResponseDto updated = clipService.reviewClip(clipId, currentUser, request);
        return ResponseEntity.ok(ApiResponseDto.ok("Clip review status updated", updated));
    }

    @Operation(
            summary = "Manually Generate Today's Challenge",
            description = "Triggers immediate generation of today's Daily Challenge. Can optionally seed 3 sample approved clips if the pool is empty.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily challenge generated successfully"),
            @ApiResponse(responseCode = "400", description = "Not enough approved clips"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    })
    @PostMapping("/daily/generate")
    public ResponseEntity<ApiResponseDto<DailyChallengeResponseDto>> generateDailyChallenge(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "If true and fewer than 3 approved clips exist, automatically seeds sample approved clips")
            @RequestParam(required = false, defaultValue = "true") boolean seedIfEmpty
    ) {
        verifyAdmin(currentUser);
        DailyChallengeEntity challenge = dailyChallengeService.manualGenerateTodayChallenge(seedIfEmpty);
        DailyChallengeResponseDto response = dailyChallengeService.getTodayChallenge(Optional.of(currentUser), null);
        return ResponseEntity.ok(ApiResponseDto.ok("Daily challenge generated successfully", response));
    }

    private void verifyAdmin(AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new AccessDeniedException("Administrative privileges required to access clip moderation");
        }
    }
}
