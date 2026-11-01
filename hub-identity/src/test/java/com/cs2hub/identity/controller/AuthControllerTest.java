package com.cs2hub.identity.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.identity.dto.AuthResponseDto;
import com.cs2hub.identity.dto.AuthTokenResponseDto;
import com.cs2hub.identity.dto.DevLoginRequestDto;
import com.cs2hub.identity.dto.LoginRequestDto;
import com.cs2hub.identity.dto.RegisterRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.service.AuthService;
import com.cs2hub.identity.service.SteamAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private SteamAuthService steamAuthService;

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(steamAuthService, authService);
    }

    @Test
    @DisplayName("GET /api/v1/auth/steam/login with redirect=true should return 302 Found")
    void shouldRedirectToSteamLogin() {
        when(steamAuthService.buildLoginRedirectUrl()).thenReturn("https://steamcommunity.com/openid/login?openid.mode=checkid_setup");

        ResponseEntity<?> response = authController.steamLogin(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("https://steamcommunity.com/openid/login?openid.mode=checkid_setup"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/steam/login with redirect=false should return 200 JSON with loginUrl")
    void shouldReturnLoginUrlJson() {
        when(steamAuthService.buildLoginRedirectUrl()).thenReturn("https://steamcommunity.com/openid/login?openid.mode=checkid_setup");

        ResponseEntity<?> response = authController.steamLogin(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ApiResponseDto.class);
    }

    @Test
    @DisplayName("GET /api/v1/auth/steam/callback with format=redirect should return 302 to frontend with token")
    void shouldRedirectFrontendOnCallback() {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, "76561198000000001", "dev1ce", null, null, null, "USER", Instant.now());
        AuthTokenResponseDto tokenResponse = AuthTokenResponseDto.bearer("jwt.test.token", 604800000L, profile);

        when(steamAuthService.verifyAndAuthenticate(any())).thenReturn(tokenResponse);
        when(steamAuthService.getFrontendRedirectUrl()).thenReturn("http://localhost:3000/auth/callback");

        ResponseEntity<?> response = authController.steamCallback(Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000001"), "redirect");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:3000/auth/callback?token=jwt.test.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/dev-login should return 200 with JWT and user profile")
    void shouldReturnTokenOnDevLogin() {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, "76561198000000001", "dev_user", null, null, null, "USER", Instant.now());
        AuthTokenResponseDto tokenResponse = AuthTokenResponseDto.bearer("jwt.test.token", 604800000L, profile);

        when(steamAuthService.devLogin(any())).thenReturn(tokenResponse);

        DevLoginRequestDto request = new DevLoginRequestDto("76561198000000001", "dev_user", "USER");
        ResponseEntity<ApiResponseDto<AuthTokenResponseDto>> response = authController.devLogin(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("jwt.test.token");
        assertThat(response.getBody().data().user().username()).isEqualTo("dev_user");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 with JWT and user profile")
    void shouldRegisterSuccessfully() {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, null, "registered_user", "reg@cs2hub.com", null, null, null, "USER", Instant.now());
        AuthResponseDto authResponse = AuthResponseDto.bearer("jwt.registered.token", 604800000L, profile);

        RegisterRequestDto request = new RegisterRequestDto("registered_user", "reg@cs2hub.com", "Password123!");
        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<ApiResponseDto<AuthResponseDto>> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("jwt.registered.token");
        assertThat(response.getBody().data().user().username()).isEqualTo("registered_user");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with JWT and user profile")
    void shouldLoginSuccessfully() {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, null, "login_user", "login@cs2hub.com", null, null, null, "USER", Instant.now());
        AuthResponseDto authResponse = AuthResponseDto.bearer("jwt.login.token", 604800000L, profile);

        LoginRequestDto request = new LoginRequestDto("login_user", "Password123!");
        when(authService.loginWithPassword(request)).thenReturn(authResponse);

        ResponseEntity<ApiResponseDto<AuthResponseDto>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("jwt.login.token");
        assertThat(response.getBody().data().user().username()).isEqualTo("login_user");
    }
}
