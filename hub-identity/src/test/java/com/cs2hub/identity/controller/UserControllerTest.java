package com.cs2hub.identity.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.identity.dto.UpdateProfileRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserLookupService userLookupService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, userLookupService);
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return current authenticated user profile")
    void shouldReturnCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser currentUser = new AuthenticatedUser(userId, "76561198000000001", "electronic", "USER");

        UserProfileDto profileDto = new UserProfileDto(
                userId, "76561198000000001", "electronic",
                "https://avatar.url", "https://profile.url", null, "USER", Instant.now()
        );

        when(userService.getCurrentUserProfile(userId)).thenReturn(profileDto);

        ResponseEntity<ApiResponseDto<UserProfileDto>> response = userController.getCurrentUser(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().username()).isEqualTo("electronic");
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me should update editable profile fields")
    void shouldUpdateCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser currentUser = new AuthenticatedUser(userId, "76561198000000001", "oldName", "USER");

        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "newName",
                "https://steamcommunity.com/tradeoffer/new/?partner=1&token=abc"
        );

        UserProfileDto updatedProfile = new UserProfileDto(
                userId, "76561198000000001", "newName",
                null, null, request.tradeUrl(), "USER", Instant.now()
        );

        when(userService.updateUserProfile(userId, request)).thenReturn(updatedProfile);

        ResponseEntity<ApiResponseDto<UserProfileDto>> response = userController.updateCurrentUser(currentUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().username()).isEqualTo("newName");
        assertThat(response.getBody().data().tradeUrl()).isEqualTo(request.tradeUrl());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} should return public UserSummaryDto")
    void shouldReturnPublicUserProfile() {
        UUID userId = UUID.randomUUID();
        UserSummaryDto summaryDto = new UserSummaryDto(userId, "76561198000000001", "donk", "https://donk.avatar");

        when(userLookupService.findById(userId)).thenReturn(Optional.of(summaryDto));

        ResponseEntity<ApiResponseDto<UserSummaryDto>> response = userController.getUserById(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().username()).isEqualTo("donk");
    }
}
