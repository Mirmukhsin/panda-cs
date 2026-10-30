package com.cs2hub.matches.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.MatchAnalysisSummaryDto;
import com.cs2hub.matches.dto.PlayerMatchHistoryItemDto;
import com.cs2hub.matches.dto.PlayerRoastCardDto;
import com.cs2hub.matches.dto.SubmitMatchRequestDto;
import com.cs2hub.matches.service.MatchAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Tag(name = "Matches & Roast Engine", description = "CS2 match demo parsing, Roast & Flex automated player badging, and shareable roast cards")
@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchAnalysisService matchAnalysisService;

    @Operation(
            summary = "Submit a match for analysis",
            description = "Submits a CS2 match share code (e.g. CSGO-xxxxx-xxxxx) or demo download link for parsing and Roast & Flex evaluation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Match queued or returned existing analysis",
                    content = @Content(schema = @Schema(implementation = MatchAnalysisDetailDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or empty share code")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> submitMatch(
            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser,

            @Valid @RequestBody SubmitMatchRequestDto request
    ) {
        MatchAnalysisDetailDto detail = matchAnalysisService.submitMatchForAnalysis(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Match submitted for analysis", detail));
    }

    @Operation(
            summary = "Simulate an analyzed match (Dev/Demo)",
            description = "Instantly generates a completed 10-player CS2 match on the specified map with realistic stats and Roast & Flex badges."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Simulated match created",
                    content = @Content(schema = @Schema(implementation = MatchAnalysisDetailDto.class))
            )
    })
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> simulateMatch(
            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser,

            @Parameter(description = "Map name (e.g. de_mirage, de_inferno, de_dust2, de_anubis, de_ancient)")
            @RequestParam(required = false, defaultValue = "de_mirage") String mapName
    ) {
        MatchAnalysisDetailDto detail = matchAnalysisService.simulateMatchAnalysis(currentUser, mapName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Simulated match generated successfully", detail));
    }

    @Operation(
            summary = "Browse recent analyzed matches",
            description = "Returns a paginated list of recently analyzed completed matches."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recent matches retrieved successfully"
            )
    })
    @GetMapping("/recent")
    public ResponseEntity<ApiResponseDto<Page<MatchAnalysisSummaryDto>>> getRecentMatches(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (1-50)")
            @RequestParam(defaultValue = "15") int size
    ) {
        Page<MatchAnalysisSummaryDto> recent = matchAnalysisService.getRecentMatches(page, size);
        return ResponseEntity.ok(ApiResponseDto.ok(recent));
    }

    @Operation(
            summary = "Get player match history and recent badges",
            description = "Retrieves match performance history and earned badges for a specific Steam64 ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Player match history retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlayerMatchHistoryItemDto.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid Steam ID")
    })
    @GetMapping("/player/{steamId}")
    public ResponseEntity<ApiResponseDto<List<PlayerMatchHistoryItemDto>>> getPlayerMatchHistory(
            @Parameter(description = "64-bit Steam ID", example = "76561198000000001")
            @PathVariable String steamId
    ) {
        List<PlayerMatchHistoryItemDto> history = matchAnalysisService.getPlayerMatchHistory(steamId);
        return ResponseEntity.ok(ApiResponseDto.ok(history));
    }

    @Operation(
            summary = "Get match analysis details",
            description = "Retrieves full match scoreboard, round summary, and all 10 players' earned Roast & Flex badges by UUID or 8-character share slug."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Match analysis retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MatchAnalysisDetailDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> getMatchByIdOrSlug(
            @Parameter(description = "Match UUID or 8-character share slug (e.g. 7K9X2B4F)")
            @PathVariable String idOrSlug
    ) {
        MatchAnalysisDetailDto detail = matchAnalysisService.getMatchByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(ApiResponseDto.ok(detail));
    }

    @Operation(
            summary = "Get player shareable roast card",
            description = "Returns a focused data payload optimized for rendering and generating a 1200x630 Discord/Twitter shareable roast card."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roast card details retrieved",
                    content = @Content(schema = @Schema(implementation = PlayerRoastCardDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Match or player not found")
    })
    @GetMapping("/{idOrSlug}/players/{steamId}/roast-card")
    public ResponseEntity<ApiResponseDto<PlayerRoastCardDto>> getPlayerRoastCard(
            @Parameter(description = "Match UUID or 8-character share slug")
            @PathVariable String idOrSlug,

            @Parameter(description = "64-bit Steam ID")
            @PathVariable String steamId
    ) {
        PlayerRoastCardDto card = matchAnalysisService.getPlayerRoastCard(idOrSlug, steamId);
        return ResponseEntity.ok(ApiResponseDto.ok(card));
    }
}
