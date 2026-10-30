package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.dto.SaveLoadoutRequestDto;
import com.cs2hub.loadouts.dto.UpvoteResponseDto;
import com.cs2hub.loadouts.dto.UserLoadoutDetailDto;
import com.cs2hub.loadouts.dto.UserLoadoutSummaryDto;
import com.cs2hub.loadouts.service.CommunityLoadoutService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Community Loadouts", description = "Community-created CS2 loadouts, custom sticker craft showcases, and upvoting")
@RestController
@RequestMapping("/api/v1/loadouts/community")
@RequiredArgsConstructor
public class CommunityLoadoutController {

    private final CommunityLoadoutService communityLoadoutService;

    @Operation(
            summary = "Browse community loadouts feed",
            description = "Returns a paginated list of public community loadouts filtered by color theme and budget, sorted by popularity or recency."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public loadouts feed retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<UserLoadoutSummaryDto>>> getPublicLoadouts(
            @Parameter(description = "Filter by theme color (e.g. RED, BLUE, PURPLE, GOLD)")
            @RequestParam(required = false) SkinColor color,

            @Parameter(description = "Maximum total cost filter in USD")
            @RequestParam(required = false) BigDecimal maxBudget,

            @Parameter(description = "Sort order ('popular' for highest upvotes, 'latest' for newest)")
            @RequestParam(defaultValue = "popular") String sortBy,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser
    ) {
        Optional<UUID> currentUserId = currentUser.map(AuthenticatedUser::userId);
        Page<UserLoadoutSummaryDto> loadouts = communityLoadoutService.getPublicLoadouts(
                color, maxBudget, sortBy, page, size, currentUserId
        );
        return ResponseEntity.ok(ApiResponseDto.ok(loadouts));
    }

    @Operation(
            summary = "Get community loadout details",
            description = "Retrieves full weapon configurations, applied sticker crafts, pricing breakdown, and author metadata for a specific loadout."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Loadout details retrieved",
                    content = @Content(schema = @Schema(implementation = UserLoadoutDetailDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Loadout not found or is private")
    })
    @GetMapping("/{loadoutId}")
    public ResponseEntity<ApiResponseDto<UserLoadoutDetailDto>> getLoadoutById(
            @Parameter(description = "Loadout UUID")
            @PathVariable UUID loadoutId,

            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser
    ) {
        Optional<UUID> currentUserId = currentUser.map(AuthenticatedUser::userId);
        UserLoadoutDetailDto detail = communityLoadoutService.getLoadoutById(loadoutId, currentUserId);
        return ResponseEntity.ok(ApiResponseDto.ok(detail));
    }

    @Operation(
            summary = "Save a new community loadout / sticker craft",
            description = "Allows an authenticated user to save and publish their weapon loadout and custom sticker crafts.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Loadout created successfully",
                    content = @Content(schema = @Schema(implementation = UserLoadoutDetailDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or unknown skin/sticker ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<UserLoadoutDetailDto>> createLoadout(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,

            @Valid @RequestBody SaveLoadoutRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to create a loadout");
        }
        UserLoadoutDetailDto created = communityLoadoutService.createLoadout(currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Community loadout published successfully", created));
    }

    @Operation(
            summary = "Get current user's saved loadouts",
            description = "Lists all loadouts created by the authenticated user, including private drafts.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User's loadouts retrieved"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<List<UserLoadoutSummaryDto>>> getMyLoadouts(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to view your loadouts");
        }
        List<UserLoadoutSummaryDto> loadouts = communityLoadoutService.getUserLoadouts(currentUser.userId());
        return ResponseEntity.ok(ApiResponseDto.ok(loadouts));
    }

    @Operation(
            summary = "Toggle upvote on a community loadout",
            description = "Adds an upvote if not already upvoted, or removes an existing upvote. Deduplicated per user.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Upvote toggled successfully",
                    content = @Content(schema = @Schema(implementation = UpvoteResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Loadout not found")
    })
    @PostMapping("/{loadoutId}/upvote")
    public ResponseEntity<ApiResponseDto<UpvoteResponseDto>> toggleUpvote(
            @Parameter(description = "Target loadout UUID")
            @PathVariable UUID loadoutId,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to upvote a loadout");
        }
        UpvoteResponseDto response = communityLoadoutService.toggleUpvote(loadoutId, currentUser.userId());
        return ResponseEntity.ok(ApiResponseDto.ok("Upvote updated", response));
    }

    @Operation(
            summary = "Delete a loadout",
            description = "Deletes a loadout. Can only be performed by the creator or an administrator.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loadout deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not loadout owner or admin"),
            @ApiResponse(responseCode = "404", description = "Loadout not found")
    })
    @DeleteMapping("/{loadoutId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteLoadout(
            @Parameter(description = "Loadout UUID to delete")
            @PathVariable UUID loadoutId,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to delete a loadout");
        }
        communityLoadoutService.deleteLoadout(loadoutId, currentUser);
        return ResponseEntity.ok(ApiResponseDto.ok("Loadout deleted successfully", null));
    }
}
