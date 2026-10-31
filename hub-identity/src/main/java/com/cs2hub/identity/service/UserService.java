package com.cs2hub.identity.service;

import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.dto.UpdateProfileRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SteamAuthService steamAuthService;

    @Transactional
    public UserProfileDto getCurrentUserProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        // Auto-Heal Existing Users on GET /api/v1/users/me:
        // If stored username starts with "Player_" and steamId starts with "7656", auto-heal via Steam summary
        if (user.getUsername() != null && user.getUsername().startsWith("Player_")
                && user.getSteamId() != null && user.getSteamId().startsWith("7656")) {
            try {
                SteamAuthService.SteamProfileSummary summary = steamAuthService.fetchSteamProfileSummary(user.getSteamId());
                if (summary != null && StringUtils.hasText(summary.username()) && !summary.username().startsWith("Player_")) {
                    log.info("Auto-healing Steam profile for user {}: old username='{}' -> new username='{}'",
                            user.getId(), user.getUsername(), summary.username());
                    user.setUsername(summary.username());
                    if (StringUtils.hasText(summary.avatarUrl())) {
                        user.setAvatarUrl(summary.avatarUrl());
                    }
                    user = userRepository.save(user);
                }
            } catch (Exception ex) {
                log.warn("Failed auto-healing Steam profile for user {}: {}", user.getId(), ex.getMessage());
            }
        }

        return steamAuthService.toUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(UUID userId, UpdateProfileRequestDto request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        if (StringUtils.hasText(request.username())) {
            user.setUsername(request.username().trim());
        }

        if (request.tradeUrl() != null) {
            String trimmedTradeUrl = request.tradeUrl().trim();
            user.setTradeUrl(trimmedTradeUrl.isEmpty() ? null : trimmedTradeUrl);
        }

        user = userRepository.save(user);
        return steamAuthService.toUserProfileDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileById(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));
        return steamAuthService.toUserProfileDto(user);
    }
}
