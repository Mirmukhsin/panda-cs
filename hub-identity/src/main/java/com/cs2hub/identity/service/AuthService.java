package com.cs2hub.identity.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.JwtTokenProvider;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.dto.AuthResponseDto;
import com.cs2hub.identity.dto.LoginRequestDto;
import com.cs2hub.identity.dto.RegisterRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String DEFAULT_LOCAL_AVATAR_BASE = "https://api.dicebear.com/7.x/bottts/svg?seed=";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SteamAuthService steamAuthService;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        String avatarUrl = DEFAULT_LOCAL_AVATAR_BASE + username;

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .avatarUrl(avatarUrl)
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user with password credentials: id='{}', username='{}', email='{}'",
                user.getId(), user.getUsername(), user.getEmail());

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto loginWithPassword(LoginRequestDto request) {
        String identifier = request.usernameOrEmail().trim();

        Optional<UserEntity> userOpt = userRepository.findByUsernameOrEmailIgnoreCase(identifier);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User '{}' not found", identifier);
            throw new UnauthorizedException("Invalid username or password");
        }

        UserEntity user = userOpt.get();

        if (!StringUtils.hasText(user.getPasswordHash())) {
            log.warn("Login failed: User '{}' has no password set (Steam-only account)", identifier);
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: Password mismatch for user '{}'", identifier);
            throw new UnauthorizedException("Invalid username or password");
        }

        log.info("User '{}' successfully logged in with password", user.getUsername());
        return createAuthResponse(user);
    }

    private AuthResponseDto createAuthResponse(UserEntity user) {
        AuthenticatedUser authUser = new AuthenticatedUser(
                user.getId(),
                user.getSteamId(),
                user.getUsername(),
                user.getRole().name()
        );

        String accessToken = jwtTokenProvider.generateToken(authUser);
        UserProfileDto profileDto = steamAuthService.toUserProfileDto(user);

        return AuthResponseDto.bearer(accessToken, jwtTokenProvider.getExpirationMs(), profileDto);
    }
}
