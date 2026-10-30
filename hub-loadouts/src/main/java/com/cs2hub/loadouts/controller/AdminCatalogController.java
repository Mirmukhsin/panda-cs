package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.loadouts.dto.SeedSummaryDto;
import com.cs2hub.loadouts.service.CatalogSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Loadouts Catalog", description = "Administrative endpoints for catalog seeding and external sync")
@RestController
@RequestMapping("/api/v1/loadouts/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CatalogSyncService catalogSyncService;

    @Operation(
            summary = "Seed curated skins & stickers",
            description = "Resets/populates catalog database with the high-quality curated seed catalog of popular CS2 skins and stickers.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Curated catalog seeded successfully",
                    content = @Content(schema = @Schema(implementation = SeedSummaryDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @PostMapping("/seed")
    public ResponseEntity<ApiResponseDto<SeedSummaryDto>> seedCatalog() {
        SeedSummaryDto summary = catalogSyncService.seedCuratedCatalog();
        return ResponseEntity.ok(ApiResponseDto.ok("Curated catalog seeded successfully", summary));
    }

    @Operation(
            summary = "Sync external ByMykel CSGO-API catalog",
            description = "Fetches the latest skins and stickers from the ByMykel/CSGO-API GitHub repository and updates the catalog.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Catalog sync completed",
                    content = @Content(schema = @Schema(implementation = SeedSummaryDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @PostMapping("/sync")
    public ResponseEntity<ApiResponseDto<SeedSummaryDto>> syncCatalog() {
        SeedSummaryDto summary = catalogSyncService.syncExternalCatalog();
        return ResponseEntity.ok(ApiResponseDto.ok("Catalog sync completed", summary));
    }
}
