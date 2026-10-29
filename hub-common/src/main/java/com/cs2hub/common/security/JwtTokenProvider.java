package com.cs2hub.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:cs2superhubsecurejwtsecretkeymustbe256bitslongorlargerforproduction!}") String secret,
            @Value("${app.jwt.expiration-ms:604800000}") long expirationMs
    ) {
        this.signingKey = initializeKey(secret);
        this.expirationMs = expirationMs;
    }

    private SecretKey initializeKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(AuthenticatedUser user) {
        return generateToken(user, this.expirationMs);
    }

    public String generateToken(AuthenticatedUser user, long customExpirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(customExpirationMs);

        return Jwts.builder()
                .subject(user.userId().toString())
                .claim("steamId", user.steamId())
                .claim("username", user.username())
                .claim("role", user.role() != null ? user.role() : AuthenticatedUser.ROLE_USER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthenticatedUser getAuthenticatedUser(String token) {
        Claims claims = extractClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String steamId = claims.get("steamId", String.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        return new AuthenticatedUser(userId, steamId, username, role);
    }

    public UUID getUserId(String token) {
        Claims claims = extractClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
