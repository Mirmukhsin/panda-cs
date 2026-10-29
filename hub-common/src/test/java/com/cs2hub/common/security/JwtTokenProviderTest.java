package com.cs2hub.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate a valid JWT and extract matching claims for AuthenticatedUser")
    void shouldGenerateAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "76561198012345678",
                "ZywOo",
                AuthenticatedUser.ROLE_USER
        );

        String token = jwtTokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        AuthenticatedUser extractedUser = jwtTokenProvider.getAuthenticatedUser(token);
        assertThat(extractedUser.userId()).isEqualTo(userId);
        assertThat(extractedUser.steamId()).isEqualTo("76561198012345678");
        assertThat(extractedUser.username()).isEqualTo("ZywOo");
        assertThat(extractedUser.role()).isEqualTo("USER");
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should detect expired JWT tokens as invalid")
    void shouldRejectExpiredToken() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "76561198099999999",
                "m0NESY",
                AuthenticatedUser.ROLE_USER
        );

        // Negative duration generates an already expired token
        String expiredToken = jwtTokenProvider.generateToken(user, -5000);

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed JWT tokens")
    void shouldRejectMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt.token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Should reject tokens with tampered signatures")
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "76561198011111111",
                "device",
                AuthenticatedUser.ROLE_USER
        );

        String token = jwtTokenProvider.generateToken(user);
        String tamperedToken = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should default role to USER if user role is null")
    void shouldDefaultRoleToUser() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "76561198022222222",
                "b1t",
                null
        );

        String token = jwtTokenProvider.generateToken(user);
        AuthenticatedUser extracted = jwtTokenProvider.getAuthenticatedUser(token);

        assertThat(extracted.role()).isEqualTo(AuthenticatedUser.ROLE_USER);
    }
}
