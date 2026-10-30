package com.cs2hub.matches.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.matches.dto.ClaimMatchJobResponseDto;
import com.cs2hub.matches.dto.FailMatchJobRequestDto;
import com.cs2hub.matches.dto.MatchAnalysisDetailDto;
import com.cs2hub.matches.dto.ParsedMatchPayloadDto;
import com.cs2hub.matches.service.MatchAnalysisService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Internal Worker Callbacks", description = "Endpoints for worker-demoparser daemon secured with X-Worker-Secret header")
@RestController
@RequestMapping("/api/v1/internal/matches")
@RequiredArgsConstructor
public class InternalWorkerMatchController {

    private final MatchAnalysisService matchAnalysisService;

    @Operation(
            summary = "Claim next queued match parsing job",
            description = "Atomically fetches the oldest QUEUED match job and transitions it to PARSING.",
            security = @SecurityRequirement(name = "WorkerSecret")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Claimed job details or null if queue is empty",
                    content = @Content(schema = @Schema(implementation = ClaimMatchJobResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid X-Worker-Secret")
    })
    @PostMapping("/claim-next")
    public ResponseEntity<ApiResponseDto<ClaimMatchJobResponseDto>> claimNextJob(
            @Parameter(description = "Worker shared secret authentication key", required = true)
            @RequestHeader(value = "X-Worker-Secret", required = false) String workerSecret
    ) {
        matchAnalysisService.validateWorkerSecret(workerSecret);
        Optional<ClaimMatchJobResponseDto> claimed = matchAnalysisService.claimNextQueuedJob();
        return ResponseEntity.ok(ApiResponseDto.ok(claimed.orElse(null)));
    }

    @Operation(
            summary = "Complete match parsing job",
            description = "Delivers parsed match scoreboards and player stats, evaluates Roast & Flex badges, and marks match COMPLETED.",
            security = @SecurityRequirement(name = "WorkerSecret")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Match finalized with Roast & Flex badges",
                    content = @Content(schema = @Schema(implementation = MatchAnalysisDetailDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid X-Worker-Secret"),
            @ApiResponse(responseCode = "404", description = "Match job not found")
    })
    @PostMapping("/{matchId}/complete")
    public ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> completeMatch(
            @Parameter(description = "Worker shared secret authentication key", required = true)
            @RequestHeader(value = "X-Worker-Secret", required = false) String workerSecret,

            @Parameter(description = "Match UUID")
            @PathVariable UUID matchId,

            @Valid @RequestBody ParsedMatchPayloadDto payload
    ) {
        matchAnalysisService.validateWorkerSecret(workerSecret);
        MatchAnalysisDetailDto completed = matchAnalysisService.completeMatchAnalysis(matchId, payload);
        return ResponseEntity.ok(ApiResponseDto.ok("Match parsing completed successfully", completed));
    }

    @Operation(
            summary = "Mark match parsing job as failed",
            description = "Records error diagnostics and marks the match status FAILED.",
            security = @SecurityRequirement(name = "WorkerSecret")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Match marked as failed",
                    content = @Content(schema = @Schema(implementation = MatchAnalysisDetailDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid X-Worker-Secret"),
            @ApiResponse(responseCode = "404", description = "Match job not found")
    })
    @PostMapping("/{matchId}/fail")
    public ResponseEntity<ApiResponseDto<MatchAnalysisDetailDto>> failMatch(
            @Parameter(description = "Worker shared secret authentication key", required = true)
            @RequestHeader(value = "X-Worker-Secret", required = false) String workerSecret,

            @Parameter(description = "Match UUID")
            @PathVariable UUID matchId,

            @RequestBody(required = false) FailMatchJobRequestDto request
    ) {
        matchAnalysisService.validateWorkerSecret(workerSecret);
        MatchAnalysisDetailDto failed = matchAnalysisService.failMatchAnalysis(matchId, request);
        return ResponseEntity.ok(ApiResponseDto.ok("Match marked as failed", failed));
    }
}
