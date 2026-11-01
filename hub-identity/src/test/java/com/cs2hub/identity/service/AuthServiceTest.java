package com.cs2hub.identity.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.JwtTokenProvider;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.dto.AuthResponseDto;
import com.cs2hub.identity.dto.LoginRequestDto;
import com.cs2hub.identity.dto.RegisterRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SteamAuthService steamAuthService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, steamAuthService);
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT with profile")
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequestDto request = new RegisterRequestDto("new_player", "new_player@cs2hub.com", "Password123!");

        when(userRepository.existsByUsernameIgnoreCase("new_player")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new_player@cs2hub.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashedPassword");

        UserEntity savedUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("new_player")
                .email("new_player@cs2hub.com")
                .passwordHash("$2a$10$hashedPassword")
                .avatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=new_player")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt.registered.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(604800000L);

        UserProfileDto profileDto = new UserProfileDto(
                savedUser.getId(), null, "new_player", "new_player@cs2hub.com",
                savedUser.getAvatarUrl(), null, null, "USER", savedUser.getCreatedAt()
        );
        when(steamAuthService.toUserProfileDto(savedUser)).thenReturn(profileDto);

        AuthResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt.registered.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("new_player");
        assertThat(response.user().email()).isEqualTo("new_player@cs2hub.com");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if username is already taken")
    void shouldThrowWhenUsernameAlreadyTaken() {
        RegisterRequestDto request = new RegisterRequestDto("taken_user", "unique@cs2hub.com", "Password123!");
        when(userRepository.existsByUsernameIgnoreCase("taken_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    @DisplayName("Should throw BadRequestException if email is already registered")
    void shouldThrowWhenEmailAlreadyRegistered() {
        RegisterRequestDto request = new RegisterRequestDto("unique_user", "taken@cs2hub.com", "Password123!");
        when(userRepository.existsByUsernameIgnoreCase("unique_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("taken@cs2hub.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    @DisplayName("Should login successfully with valid username and password")
    void shouldLoginSuccessfullyWithUsername() {
        LoginRequestDto request = new LoginRequestDto("valid_user", "CorrectPassword123");

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("valid_user")
                .email("valid_user@cs2hub.com")
                .passwordHash("$2a$10$hashedPassword")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsernameOrEmailIgnoreCase("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt.login.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(604800000L);

        UserProfileDto profileDto = new UserProfileDto(
                user.getId(), null, "valid_user", "valid_user@cs2hub.com",
                null, null, null, "USER", user.getCreatedAt()
        );
        when(steamAuthService.toUserProfileDto(user)).thenReturn(profileDto);

        AuthResponseDto response = authService.loginWithPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt.login.token");
        assertThat(response.user().username()).isEqualTo("valid_user");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user not found")
    void shouldThrowUnauthorizedWhenUserNotFound() {
        LoginRequestDto request = new LoginRequestDto("unknown_user", "Password123");
        when(userRepository.findByUsernameOrEmailIgnoreCase("unknown_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithPassword(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when account has no password (Steam-only)")
    void shouldThrowUnauthorizedWhenAccountHasNoPassword() {
        LoginRequestDto request = new LoginRequestDto("steam_user", "Password123");

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .steamId("76561198000000001")
                .username("steam_user")
                .passwordHash(null)
                .role(UserRole.USER)
                .build();

        when(userRepository.findByUsernameOrEmailIgnoreCase("steam_user")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.loginWithPassword(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when password does not match")
    void shouldThrowUnauthorizedWhenPasswordMismatch() {
        LoginRequestDto request = new LoginRequestDto("valid_user", "WrongPassword");

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("valid_user")
                .passwordHash("$2a$10$hashedPassword")
                .role(UserRole.USER)
                .build();

        when(userRepository.findByUsernameOrEmailIgnoreCase("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginWithPassword(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
