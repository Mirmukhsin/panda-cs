package com.cs2hub.minigames.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.minigames.dto.ClipSubmissionResponseDto;
import com.cs2hub.minigames.dto.SubmitClipRequestDto;
import com.cs2hub.minigames.service.ClipService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Minigames Clips", description = "Community clip submissions for the daily 'Guess the Elo' challenge")
@RestController
@RequestMapping("/api/v1/minigames/clips")
@RequiredArgsConstructor
public class ClipController {

    private final ClipService clipService;

    @Operation(
            summary = "Submit a gameplay clip",
            description = "Allows an authenticated user to submit a CS2 gameplay video clip to be queued for moderation review.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Clip successfully submitted",
                    content = @Content(schema = @Schema(implementation = ClipSubmissionResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error in request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ClipSubmissionResponseDto>> submitClip(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody SubmitClipRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to submit clips");
        }

        ClipSubmissionResponseDto response = clipService.submitClip(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Clip submitted successfully for moderation review", response));
    }

    @Operation(
            summary = "Get current user submitted clips",
            description = "Retrieves the list of clips submitted by the authenticated player and their current status.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clips retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<List<ClipSubmissionResponseDto>>> getMyClips(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to view your clips");
        }

        List<ClipSubmissionResponseDto> clips = clipService.getMyClips(currentUser);
        return ResponseEntity.ok(ApiResponseDto.ok(clips));
    }
}
