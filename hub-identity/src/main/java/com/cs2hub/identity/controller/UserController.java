package com.cs2hub.identity.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.identity.dto.UpdateProfileRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.service.UserService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "User Profiles", description = "Endpoints for managing user accounts, trade URLs, and public profiles")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserLookupService userLookupService;

    @Operation(
            summary = "Get Current Authenticated User Profile",
            description = "Retrieves full profile details of the currently authenticated player, automatically resolving real Steam nickname and avatar.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserProfileDto>> getCurrentUser(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to view your profile");
        }

        UserProfileDto profile = userService.getCurrentUserProfile(currentUser.userId());
        return ResponseEntity.ok(ApiResponseDto.ok(profile));
    }

    @Operation(
            summary = "Update Current User Profile",
            description = "Updates editable profile fields (such as display username or Steam trade URL).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid profile fields"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponseDto<UserProfileDto>> updateCurrentUser(
            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to update profile");
        }

        UserProfileDto updated = userService.updateUserProfile(currentUser.userId(), request);
        return ResponseEntity.ok(ApiResponseDto.ok("Profile updated successfully", updated));
    }

    @Operation(
            summary = "Get Public User Profile",
            description = "Publicly accessible endpoint returning summary profile info for any registered player."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User summary profile",
                    content = @Content(schema = @Schema(implementation = UserSummaryDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserSummaryDto>> getUserById(
            @Parameter(description = "User unique identifier")
            @PathVariable UUID userId
    ) {
        UserSummaryDto summary = userLookupService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return ResponseEntity.ok(ApiResponseDto.ok(summary));
    }
}
