package com.cs2hub.loadouts.service;

import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SkinDto;
import com.cs2hub.loadouts.dto.StickerDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.SkinSpecifications;
import com.cs2hub.loadouts.repository.StickerRepository;
import com.cs2hub.loadouts.repository.StickerSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final SkinRepository skinRepository;
    private final StickerRepository stickerRepository;

    public Page<SkinDto> searchSkins(
            SkinColor color,
            String weaponId,
            WeaponCategory category,
            TeamSide teamSide,
            BigDecimal maxPrice,
            String query,
            String rarity,
            String skinClass,
            Pageable pageable
    ) {
        return skinRepository.findAll(
                SkinSpecifications.filter(color, weaponId, category, teamSide, maxPrice, query, rarity, skinClass),
                pageable
        ).map(SkinDto::fromEntity);
    }

    public Page<SkinDto> searchSkins(
            SkinColor color,
            String weaponId,
            WeaponCategory category,
            TeamSide teamSide,
            BigDecimal maxPrice,
            String query,
            Pageable pageable
    ) {
        return searchSkins(color, weaponId, category, teamSide, maxPrice, query, null, null, pageable);
    }

    public SkinDto getSkinById(String skinId) {
        return skinRepository.findById(skinId)
                .map(SkinDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Skin not found with ID: " + skinId));
    }

    public Page<StickerDto> searchStickers(
            SkinColor color,
            BigDecimal maxPrice,
            String query,
            Pageable pageable
    ) {
        return stickerRepository.findAll(
                StickerSpecifications.filter(color, maxPrice, query),
                pageable
        ).map(StickerDto::fromEntity);
    }

    public StickerDto getStickerById(String stickerId) {
        return stickerRepository.findById(stickerId)
                .map(StickerDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker not found with ID: " + stickerId));
    }
}
