package com.cs2hub.stratboard.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.stratboard.dto.CreateStratRoomRequestDto;
import com.cs2hub.stratboard.dto.SaveRoomTacticRequestDto;
import com.cs2hub.stratboard.dto.StratRoomSnapshotDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.cs2hub.stratboard.service.StratRoomService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Stratboard Live Rooms", description = "Real-time collaborative tactical whiteboard sessions with WebSockets")
@RestController
@RequestMapping("/api/v1/stratboard/rooms")
@RequiredArgsConstructor
public class StratRoomController {

    private final StratRoomService stratRoomService;

    @Operation(
            summary = "Create a collaborative tactical room",
            description = "Initializes an interactive real-time whiteboard room and returns the 8-character join code and initial room snapshot. Zero login friction (works for both guests and authenticated players)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Collaborative room created",
                    content = @Content(schema = @Schema(implementation = StratRoomSnapshotDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or unsupported map name")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> createRoom(
            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser,

            @Valid @RequestBody CreateStratRoomRequestDto request
    ) {
        StratRoomSnapshotDto snapshot = stratRoomService.createRoom(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Collaborative room created successfully", snapshot));
    }

    @Operation(
            summary = "Get live room snapshot",
            description = "Retrieves full current board state, active participants, and all drawn elements by room code. Used on initial page join."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Room snapshot retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StratRoomSnapshotDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Room not found or expired")
    })
    @GetMapping("/{roomCode}")
    public ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> getRoomSnapshot(
            @Parameter(description = "8-character room join code (e.g. X7K9M2P4)")
            @PathVariable String roomCode
    ) {
        StratRoomSnapshotDto snapshot = stratRoomService.getRoomSnapshot(roomCode);
        return ResponseEntity.ok(ApiResponseDto.ok(snapshot));
    }

    @Operation(
            summary = "Load saved tactic into active room",
            description = "Replaces the live room's canvas elements with a saved playbook and broadcasts a LOAD_TACTIC event to all connected teammates."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tactic loaded into room",
                    content = @Content(schema = @Schema(implementation = StratRoomSnapshotDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Room or tactic not found")
    })
    @PostMapping("/{roomCode}/load-tactic/{idOrShareCode}")
    public ResponseEntity<ApiResponseDto<StratRoomSnapshotDto>> loadTactic(
            @Parameter(description = "8-character room join code")
            @PathVariable String roomCode,

            @Parameter(description = "Saved tactic UUID or 8-character share code")
            @PathVariable String idOrShareCode,

            @Parameter(hidden = true)
            @CurrentUser Optional<AuthenticatedUser> currentUser
    ) {
        StratRoomSnapshotDto snapshot = stratRoomService.loadTacticIntoRoom(roomCode, idOrShareCode, currentUser);
        return ResponseEntity.ok(ApiResponseDto.ok("Tactic loaded into room successfully", snapshot));
    }

    @Operation(
            summary = "Save active room as persistent tactic playbook",
            description = "Persists the current board's drawn elements and map state into PostgreSQL as a permanent playbook tactic.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Room persisted as tactic playbook",
                    content = @Content(schema = @Schema(implementation = TacticDetailDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required to save playbooks"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @PostMapping("/{roomCode}/save-tactic")
    public ResponseEntity<ApiResponseDto<TacticDetailDto>> saveRoomAsTactic(
            @Parameter(description = "8-character room join code")
            @PathVariable String roomCode,

            @Parameter(hidden = true)
            @CurrentUser AuthenticatedUser currentUser,

            @Valid @RequestBody SaveRoomTacticRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to save a tactic playbook");
        }
        TacticDetailDto savedTactic = stratRoomService.saveRoomAsTactic(roomCode, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Room persisted as saved tactic successfully", savedTactic));
    }
}
