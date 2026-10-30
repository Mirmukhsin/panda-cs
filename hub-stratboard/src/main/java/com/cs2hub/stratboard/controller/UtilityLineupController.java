package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.UtilityType;
import com.cs2hub.stratboard.dto.SubmitLineupRequestDto;
import com.cs2hub.stratboard.dto.UtilityLineupDto;
import com.cs2hub.stratboard.service.UtilityLineupService;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Stratboard Utility Lineups", description = "CS2 grenade lineups library for smokes, flashes, molotovs, and HE grenades with radar coordinates")
@RestController
@RequestMapping("/api/v1/stratboard/lineups")
@RequiredArgsConstructor
public class UtilityLineupController {

    private final UtilityLineupService utilityLineupService;

    @Operation(
            summary = "Browse grenade lineups catalog",
            description = "Returns grenade lineups filtered by map (e.g. de_mirage), grenade type (SMOKE, FLASH, MOLOTOV, HE), and executing side (T, CT)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lineups retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UtilityLineupDto.class)))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<UtilityLineupDto>>> getLineups(
            @Parameter(description = "Map identifier (e.g. de_mirage, de_inferno, de_dust2, de_nuke, de_ancient, de_anubis)")
            @RequestParam(required = false) String mapName,

            @Parameter(description = "Grenade type filter (SMOKE, FLASH, MOLOTOV, HE)")
            @RequestParam(required = false) UtilityType utilityType,

            @Parameter(description = "Team side filter (T, CT)")
            @RequestParam(required = false) StratSide teamSide
    ) {
        List<UtilityLineupDto> lineups = utilityLineupService.getLineups(mapName, utilityType, teamSide);
        return ResponseEntity.ok(ApiResponseDto.ok(lineups));
    }

    @Operation(
            summary = "Get lineup by ID",
            description = "Retrieves complete throw alignment instructions, radar standing and landing coordinates, and video clip URL for a single grenade lineup."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lineup details retrieved",
                    content = @Content(schema = @Schema(implementation = UtilityLineupDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Lineup not found")
    })
    @GetMapping("/{lineupId}")
    public ResponseEntity<ApiResponseDto<UtilityLineupDto>> getLineupById(
            @Parameter(description = "Lineup UUID")
            @PathVariable UUID lineupId
    ) {
        UtilityLineupDto lineup = utilityLineupService.getLineupById(lineupId);
        return ResponseEntity.ok(ApiResponseDto.ok(lineup));
    }

    @Operation(
            summary = "Submit a new utility lineup",
            description = "Allows an authenticated community member to submit a grenade lineup for moderation review and radar placement.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Lineup submitted successfully",
                    content = @Content(schema = @Schema(implementation = UtilityLineupDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request coordinates or parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<UtilityLineupDto>> submitLineup(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,

            @Valid @RequestBody SubmitLineupRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to submit utility lineups");
        }
        UtilityLineupDto created = utilityLineupService.submitLineup(currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Utility lineup submitted successfully", created));
    }

    @Operation(
            summary = "Delete utility lineup",
            description = "Allows the submitter or an administrator to delete a utility lineup.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lineup deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not owner or admin"),
            @ApiResponse(responseCode = "404", description = "Lineup not found")
    })
    @DeleteMapping("/{lineupId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteLineup(
            @Parameter(description = "Lineup UUID to delete")
            @PathVariable UUID lineupId,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to delete lineups");
        }
        utilityLineupService.deleteLineup(lineupId, currentUser);
        return ResponseEntity.ok(ApiResponseDto.ok("Utility lineup deleted successfully", null));
    }
}
