package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.dto.SaveTacticRequestDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.cs2hub.stratboard.dto.TacticSummaryDto;
import com.cs2hub.stratboard.service.TacticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Stratboard Tactics", description = "Team tactics and playbooks with interactive 2D canvas drawing states")
@RestController
@RequestMapping("/api/v1/stratboard/tactics")
@RequiredArgsConstructor
public class TacticController {

    private final TacticService tacticService;

    @Operation(
            summary = "Browse public tactics playbooks",
            description = "Returns a paginated list of community tactics filtered by map and side, sorted by upvotes and recency."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public tactics retrieved successfully"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<TacticSummaryDto>>> getPublicTactics(
            @Parameter(description = "Map identifier (e.g. de_mirage, de_inferno, de_dust2, de_nuke, de_ancient, de_anubis)")
            @RequestParam(required = false) String mapName,

            @Parameter(description = "Team side filter (T, CT)")
            @RequestParam(required = false) StratSide teamSide,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TacticSummaryDto> tactics = tacticService.getPublicTactics(mapName, teamSide, page, size);
        return ResponseEntity.ok(ApiResponseDto.ok(tactics));
    }

    @Operation(
            summary = "Get current user's saved tactics",
            description = "Retrieves all personal playbooks (public and private) authored by the authenticated user.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User tactics retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TacticSummaryDto.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<List<TacticSummaryDto>>> getMyTactics(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to view your tactics");
        }
        List<TacticSummaryDto> userTactics = tacticService.getUserTactics(currentUser.userId());
        return ResponseEntity.ok(ApiResponseDto.ok(userTactics));
    }

    @Operation(
            summary = "Get tactic playbook details by ID or share code",
            description = "Retrieves full 2D interactive canvas state and annotations by tactic UUID or 8-character share code."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tactic details retrieved",
                    content = @Content(schema = @Schema(implementation = TacticDetailDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Tactic not found or is private")
    })
    @GetMapping("/{idOrShareCode}")
    public ResponseEntity<ApiResponseDto<TacticDetailDto>> getTacticByIdOrShareCode(
            @Parameter(description = "Tactic UUID or 8-character share code (e.g. 7K9X2B4F)")
            @PathVariable String idOrShareCode,

            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser
    ) {
        Optional<UUID> currentUserId = (currentUser != null && currentUser.isPresent())
                ? currentUser.map(AuthenticatedUser::userId)
                : Optional.empty();

        TacticDetailDto tactic = tacticService.getTacticByIdOrShareCode(idOrShareCode, currentUserId);
        return ResponseEntity.ok(ApiResponseDto.ok(tactic));
    }

    @Operation(
            summary = "Create and save a new tactic playbook",
            description = "Saves an interactive 2D whiteboard stratboard state with drawings, icons, smokes, and movement lines.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tactic created successfully",
                    content = @Content(schema = @Schema(implementation = TacticDetailDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or canvas state missing"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<TacticDetailDto>> createTactic(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,

            @Valid @RequestBody SaveTacticRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to save a tactic");
        }
        TacticDetailDto created = tacticService.createTactic(currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(created));
    }

    @Operation(
            summary = "Update an existing tactic playbook",
            description = "Allows the tactic creator or an administrator to modify tactic details and canvas state.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tactic updated successfully",
                    content = @Content(schema = @Schema(implementation = TacticDetailDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the tactic author or admin"),
            @ApiResponse(responseCode = "404", description = "Tactic not found")
    })
    @PutMapping("/{tacticId}")
    public ResponseEntity<ApiResponseDto<TacticDetailDto>> updateTactic(
            @Parameter(description = "Tactic UUID")
            @PathVariable UUID tacticId,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,

            @Valid @RequestBody SaveTacticRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to edit a tactic");
        }
        TacticDetailDto updated = tacticService.updateTactic(tacticId, currentUser, request);
        return ResponseEntity.ok(ApiResponseDto.ok(updated));
    }

    @Operation(
            summary = "Delete a tactic playbook",
            description = "Allows the tactic creator or an administrator to delete a tactic playbook.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tactic deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the tactic author or admin"),
            @ApiResponse(responseCode = "404", description = "Tactic not found")
    })
    @DeleteMapping("/{tacticId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteTactic(
            @Parameter(description = "Tactic UUID")
            @PathVariable UUID tacticId,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to delete a tactic");
        }
        tacticService.deleteTactic(tacticId, currentUser);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }
}
