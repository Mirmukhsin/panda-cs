package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.loadouts.dto.GenerateLoadoutRequestDto;
import com.cs2hub.loadouts.dto.GeneratedLoadoutResponseDto;
import com.cs2hub.loadouts.service.BudgetLoadoutGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Loadouts Generator", description = "Color-matched budget CS2 loadout generator algorithm")
@RestController
@RequestMapping("/api/v1/loadouts")
@RequiredArgsConstructor
public class LoadoutGeneratorController {

    private final BudgetLoadoutGeneratorService generatorService;

    @Operation(
            summary = "Generate a color-matched budget loadout",
            description = "Public algorithmic endpoint that generates a cohesive CS2 loadout given a target budget (e.g. $50, $150), a theme color, and weapon preferences. No authentication required."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Loadout generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedLoadoutResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters or budget below $5.00")
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDto<GeneratedLoadoutResponseDto>> generateLoadout(
            @Valid @RequestBody GenerateLoadoutRequestDto request
    ) {
        GeneratedLoadoutResponseDto loadout = generatorService.generateLoadout(request);
        return ResponseEntity.ok(ApiResponseDto.ok("Color-matched budget loadout generated successfully", loadout));
    }
}
