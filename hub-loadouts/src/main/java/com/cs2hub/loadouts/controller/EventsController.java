package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.CurrentUser;
import com.cs2hub.loadouts.dto.EnterGiveawayResponseDto;
import com.cs2hub.loadouts.dto.EventsHubResponseDto;
import com.cs2hub.loadouts.service.EventsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Events & Deals", description = "CS2 active giveaways, marketplace deals, and special tournament events")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;

    @Operation(
            summary = "Get Events & Deals Hub data",
            description = "Returns aggregated active giveaways, curated marketplace discount deals, and ongoing special CS2 events."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Events hub data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventsHubResponseDto.class))
            )
    })
    @GetMapping("/hub")
    public ResponseEntity<ApiResponseDto<EventsHubResponseDto>> getEventsHub(
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "Optional guest session identifier for tracking entries")
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId
    ) {
        String userKey = resolveUserOrGuestKey(currentUser, guestSessionId);
        EventsHubResponseDto hub = eventsService.getEventsHub(userKey);
        return ResponseEntity.ok(ApiResponseDto.ok(hub));
    }

    @Operation(
            summary = "Enter an active giveaway",
            description = "Atomically records an entry for authenticated user or anonymous guest, incrementing the total entry count."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully entered giveaway",
                    content = @Content(schema = @Schema(implementation = EnterGiveawayResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Giveaway not found"
            )
    })
    @PostMapping("/giveaways/{id}/enter")
    public ResponseEntity<ApiResponseDto<EnterGiveawayResponseDto>> enterGiveaway(
            @Parameter(description = "Giveaway ID to enter", example = "giveaway-ak47-bloodsport")
            @PathVariable("id") String id,
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "Optional guest session identifier for tracking entries")
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId
    ) {
        String userKey = resolveUserOrGuestKey(currentUser, guestSessionId);
        EnterGiveawayResponseDto response = eventsService.enterGiveaway(id, userKey);
        return ResponseEntity.ok(ApiResponseDto.ok(response));
    }

    private String resolveUserOrGuestKey(AuthenticatedUser currentUser, String guestSessionId) {
        if (currentUser != null && currentUser.userId() != null) {
            return "user-" + currentUser.userId();
        }
        if (guestSessionId != null && !guestSessionId.isBlank()) {
            String trimmed = guestSessionId.trim();
            return trimmed.startsWith("guest-") ? trimmed : "guest-" + trimmed;
        }
        return "";
    }
}
