package com.cs2hub.identity.service;

import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SteamAuthService steamAuthService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, steamAuthService);
    }

    @Test
    @DisplayName("Should auto-heal user profile on GET /me when username starts with Player_")
    void shouldAutoHealProfileWhenUsernameStartsWithPlayer_() {
        UUID userId = UUID.randomUUID();
        String steamId = "76561198000000001";

        UserEntity userEntity = UserEntity.builder()
                .id(userId)
                .steamId(steamId)
                .username("Player_967284")
                .avatarUrl("https://avatars.steamstatic.com/default.jpg")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        SteamAuthService.SteamProfileSummary realSummary = new SteamAuthService.SteamProfileSummary(
                "PandaMaster",
                "https://avatars.steamstatic.com/real_avatar_full.jpg",
                "https://steamcommunity.com/profiles/" + steamId
        );

        UserProfileDto healedDto = new UserProfileDto(
                userId, steamId, "PandaMaster",
                "https://avatars.steamstatic.com/real_avatar_full.jpg",
                "https://steamcommunity.com/profiles/" + steamId,
                null, "USER", Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(steamAuthService.fetchSteamProfileSummary(steamId)).thenReturn(realSummary);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(steamAuthService.toUserProfileDto(userEntity)).thenReturn(healedDto);

        UserProfileDto result = userService.getCurrentUserProfile(userId);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("PandaMaster");
        assertThat(result.avatarUrl()).isEqualTo("https://avatars.steamstatic.com/real_avatar_full.jpg");

        verify(steamAuthService).fetchSteamProfileSummary(steamId);
        verify(userRepository).save(userEntity);
        assertThat(userEntity.getUsername()).isEqualTo("PandaMaster");
        assertThat(userEntity.getAvatarUrl()).isEqualTo("https://avatars.steamstatic.com/real_avatar_full.jpg");
    }

    @Test
    @DisplayName("Should not auto-heal when username is already custom")
    void shouldNotAutoHealWhenUsernameIsCustom() {
        UUID userId = UUID.randomUUID();
        String steamId = "76561198000000002";

        UserEntity userEntity = UserEntity.builder()
                .id(userId)
                .steamId(steamId)
                .username("s1mple_pro")
                .avatarUrl("https://avatars.steamstatic.com/s1mple.jpg")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        UserProfileDto profileDto = new UserProfileDto(
                userId, steamId, "s1mple_pro",
                "https://avatars.steamstatic.com/s1mple.jpg",
                "https://steamcommunity.com/profiles/" + steamId,
                null, "USER", Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(steamAuthService.toUserProfileDto(userEntity)).thenReturn(profileDto);

        UserProfileDto result = userService.getCurrentUserProfile(userId);

        assertThat(result.username()).isEqualTo("s1mple_pro");
        verify(steamAuthService, never()).fetchSteamProfileSummary(any());
        verify(userRepository, never()).save(any());
    }
}
