package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SkinDto;
import com.cs2hub.loadouts.dto.StickerDto;
import com.cs2hub.loadouts.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Loadouts Catalog", description = "CS2 weapon skins and stickers catalog search and filter APIs")
@RestController
@RequestMapping("/api/v1/loadouts/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(
            summary = "Search and filter weapon skins",
            description = "Returns a paginated list of CS2 weapon skins filtered by color theme, weapon, category, team side, max price, rarity, skin class, or search text."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of weapon skins retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/skins")
    public ResponseEntity<ApiResponseDto<Page<SkinDto>>> searchSkins(
            @Parameter(description = "Primary color theme filter (e.g. RED, BLUE, PURPLE, GREEN, GOLD)")
            @RequestParam(required = false) SkinColor color,

            @Parameter(description = "Weapon technical identifier (e.g. weapon_ak47, weapon_m4a1_silencer, weapon_awp)")
            @RequestParam(required = false) String weaponId,

            @Parameter(description = "Weapon category (PISTOLS, RIFLES, SMGS, HEAVY, KNIVES, GLOVES)")
            @RequestParam(required = false) WeaponCategory category,

            @Parameter(description = "Playable team side (T, CT, BOTH)")
            @RequestParam(required = false) TeamSide teamSide,

            @Parameter(description = "Maximum budget ceiling price in USD")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Free-text search query matching skin display name")
            @RequestParam(required = false) String q,

            @Parameter(description = "Item rarity grade filter (e.g. Covert, Classified, Restricted, Mil-Spec Grade)")
            @RequestParam(required = false) String rarity,

            @Parameter(description = "Skin class filter (e.g. COVERT_RIFLE, CLASSIFIED_PISTOL, ALL_COVERT)")
            @RequestParam(required = false) String skinClass,

            @Parameter(description = "Zero-based page index (defaults to 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (defaults to 20, max 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort property and direction, e.g. lowestPriceUsd,asc or name,asc")
            @RequestParam(defaultValue = "lowestPriceUsd,asc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort, "lowestPriceUsd");
        Page<SkinDto> results = (rarity != null || skinClass != null)
                ? catalogService.searchSkins(color, weaponId, category, teamSide, maxPrice, q, rarity, skinClass, pageable)
                : catalogService.searchSkins(color, weaponId, category, teamSide, maxPrice, q, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(results));
    }

    public ResponseEntity<ApiResponseDto<Page<SkinDto>>> searchSkins(
            SkinColor color,
            String weaponId,
            WeaponCategory category,
            TeamSide teamSide,
            BigDecimal maxPrice,
            String q,
            int page,
            int size,
            String sort
    ) {
        return searchSkins(color, weaponId, category, teamSide, maxPrice, q, null, null, page, size, sort);
    }

    @Operation(
            summary = "Get skin details by ID",
            description = "Retrieves complete metadata and wear-specific pricing for a single weapon skin."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Skin details retrieved",
                    content = @Content(schema = @Schema(implementation = SkinDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Skin not found")
    })
    @GetMapping("/skins/{skinId}")
    public ResponseEntity<ApiResponseDto<SkinDto>> getSkinById(
            @Parameter(description = "Unique skin identifier (e.g. skin-ak47-asiimov)")
            @PathVariable String skinId
    ) {
        SkinDto skin = catalogService.getSkinById(skinId);
        return ResponseEntity.ok(ApiResponseDto.ok(skin));
    }

    @Operation(
            summary = "Search and filter stickers",
            description = "Returns a paginated list of CS2 stickers filtered by color theme, max price, or search text."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of stickers retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/stickers")
    public ResponseEntity<ApiResponseDto<Page<StickerDto>>> searchStickers(
            @Parameter(description = "Primary color theme filter (e.g. RED, BLUE, GOLD)")
            @RequestParam(required = false) SkinColor color,

            @Parameter(description = "Maximum sticker price ceiling in USD")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Free-text search query matching sticker display name")
            @RequestParam(required = false) String q,

            @Parameter(description = "Zero-based page index (defaults to 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (defaults to 20, max 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort property and direction, e.g. priceUsd,asc or name,asc")
            @RequestParam(defaultValue = "priceUsd,asc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort, "priceUsd");
        Page<StickerDto> results = catalogService.searchStickers(color, maxPrice, q, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(results));
    }

    @Operation(
            summary = "Get sticker details by ID",
            description = "Retrieves metadata and pricing for a single CS2 tournament or community sticker."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sticker details retrieved",
                    content = @Content(schema = @Schema(implementation = StickerDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "Sticker not found")
    })
    @GetMapping("/stickers/{stickerId}")
    public ResponseEntity<ApiResponseDto<StickerDto>> getStickerById(
            @Parameter(description = "Unique sticker identifier")
            @PathVariable String stickerId
    ) {
        StickerDto sticker = catalogService.getStickerById(stickerId);
        return ResponseEntity.ok(ApiResponseDto.ok(sticker));
    }

    private Pageable createPageable(int page, int size, String sort, String defaultProperty) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, 1), 100);

        Sort sortObj = Sort.by(Sort.Direction.ASC, defaultProperty);
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            }
            sortObj = Sort.by(direction, property);
        }

        return PageRequest.of(validatedPage, validatedSize, sortObj);
    }
}
