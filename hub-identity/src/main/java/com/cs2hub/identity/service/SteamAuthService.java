package com.cs2hub.identity.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.common.security.JwtTokenProvider;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.dto.AuthTokenResponseDto;
import com.cs2hub.identity.dto.DevLoginRequestDto;
import com.cs2hub.identity.dto.UserProfileDto;
import com.cs2hub.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SteamAuthService {

    public static final String STEAM_OPENID_URL = "https://steamcommunity.com/openid/login";
    public static final String DEFAULT_AVATAR_URL = "https://avatars.steamstatic.com/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg";
    private static final Pattern STEAM_CLAIMED_ID_PATTERN = Pattern.compile("^https?://steamcommunity\\.com/openid/id/(7656119\\d{10})$");

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String realm;
    private final String returnTo;
    private final String frontendRedirectUrl;
    private final boolean devModeEnabled;

    public SteamAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.steam.api-key:}") String apiKey,
            @Value("${app.steam.realm:http://localhost:8080}") String realm,
            @Value("${app.steam.return-to:http://localhost:8080/api/v1/auth/steam/callback}") String returnTo,
            @Value("${app.steam.frontend-redirect-url:http://localhost:3000/auth/callback}") String frontendRedirectUrl,
            @Value("${app.auth.dev-mode-enabled:true}") boolean devModeEnabled
    ) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restClient = restClientBuilder != null ? restClientBuilder.build() : RestClient.create();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.realm = realm;
        this.returnTo = returnTo;
        this.frontendRedirectUrl = frontendRedirectUrl;
        this.devModeEnabled = devModeEnabled;
    }

    public String buildLoginRedirectUrl() {
        return UriComponentsBuilder.fromHttpUrl(STEAM_OPENID_URL)
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", returnTo)
                .queryParam("openid.realm", realm)
                .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
                .toUriString();
    }

    @Transactional
    public AuthTokenResponseDto verifyAndAuthenticate(Map<String, String> queryParams) {
        String claimedId = queryParams.get("openid.claimed_id");
        if (!StringUtils.hasText(claimedId)) {
            throw new BadRequestException("Missing openid.claimed_id in callback parameters");
        }

        Matcher matcher = STEAM_CLAIMED_ID_PATTERN.matcher(claimedId);
        if (!matcher.matches()) {
            throw new BadRequestException("Invalid Steam claimed_id format: " + claimedId);
        }
        String steamId = matcher.group(1);

        verifyWithSteamOpenId(queryParams);

        SteamPlayerProfile playerProfile = fetchSteamPlayerProfile(steamId);

        UserEntity user = upsertUser(steamId, playerProfile);

        return createAuthTokenResponse(user);
    }

    @Transactional
    public AuthTokenResponseDto devLogin(DevLoginRequestDto request) {
        if (!devModeEnabled) {
            throw new UnauthorizedException("Dev login is disabled in this environment");
        }

        UserRole role = UserRole.USER;
        if (StringUtils.hasText(request.role())) {
            try {
                role = UserRole.valueOf(request.role().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role specified in dev login: {}, defaulting to USER", request.role());
            }
        }

        final UserRole finalRole = role;
        UserEntity user = userRepository.findBySteamId(request.steamId())
                .map(existing -> {
                    existing.setUsername(request.username());
                    existing.setRole(finalRole);
                    return existing;
                })
                .orElseGet(() -> UserEntity.builder()
                        .id(UUID.randomUUID())
                        .steamId(request.steamId())
                        .username(request.username())
                        .avatarUrl(DEFAULT_AVATAR_URL)
                        .profileUrl("https://steamcommunity.com/profiles/" + request.steamId())
                        .role(finalRole)
                        .build());

        user = userRepository.save(user);

        return createAuthTokenResponse(user);
    }

    private void verifyWithSteamOpenId(Map<String, String> queryParams) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getKey().startsWith("openid.")) {
                form.add(entry.getKey(), entry.getValue());
            }
        }
        form.set("openid.mode", "check_authentication");

        try {
            String responseBody = restClient.post()
                    .uri(STEAM_OPENID_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || !responseBody.contains("is_valid:true")) {
                log.warn("Steam OpenID verification response invalid: {}", responseBody);
                throw new UnauthorizedException("Steam OpenID verification failed");
            }
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed communicating with Steam OpenID server: {}", ex.getMessage());
            throw new UnauthorizedException("Failed communicating with Steam authentication server");
        }
    }

    private SteamPlayerProfile fetchSteamPlayerProfile(String steamId) {
        if (!StringUtils.hasText(apiKey)) {
            log.debug("No Steam API key configured, falling back to default profile for steamId: {}", steamId);
            return defaultProfile(steamId);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/")
                    .queryParam("key", apiKey)
                    .queryParam("steamids", steamId)
                    .toUriString();

            String responseJson = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (StringUtils.hasText(responseJson)) {
                JsonNode root = objectMapper.readTree(responseJson);
                JsonNode players = root.path("response").path("players");
                if (players.isArray() && !players.isEmpty()) {
                    JsonNode player = players.get(0);
                    String personaName = player.path("personaname").asText(null);
                    String avatarFull = player.path("avatarfull").asText(null);
                    String profileUrl = player.path("profileurl").asText(null);

                    return new SteamPlayerProfile(
                            StringUtils.hasText(personaName) ? personaName : defaultUsername(steamId),
                            StringUtils.hasText(avatarFull) ? avatarFull : DEFAULT_AVATAR_URL,
                            StringUtils.hasText(profileUrl) ? profileUrl : defaultProfileUrl(steamId)
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch player profile from Steam Web API: {}", ex.getMessage());
        }

        return defaultProfile(steamId);
    }

    private UserEntity upsertUser(String steamId, SteamPlayerProfile profile) {
        UserEntity user = userRepository.findBySteamId(steamId)
                .map(existing -> {
                    existing.setUsername(profile.username());
                    existing.setAvatarUrl(profile.avatarUrl());
                    existing.setProfileUrl(profile.profileUrl());
                    return existing;
                })
                .orElseGet(() -> UserEntity.builder()
                        .id(UUID.randomUUID())
                        .steamId(steamId)
                        .username(profile.username())
                        .avatarUrl(profile.avatarUrl())
                        .profileUrl(profile.profileUrl())
                        .role(UserRole.USER)
                        .build());

        return userRepository.save(user);
    }

    private AuthTokenResponseDto createAuthTokenResponse(UserEntity user) {
        AuthenticatedUser authUser = new AuthenticatedUser(
                user.getId(),
                user.getSteamId(),
                user.getUsername(),
                user.getRole().name()
        );

        String accessToken = jwtTokenProvider.generateToken(authUser);
        UserProfileDto profileDto = toUserProfileDto(user);

        return AuthTokenResponseDto.bearer(accessToken, jwtTokenProvider.getExpirationMs(), profileDto);
    }

    public UserProfileDto toUserProfileDto(UserEntity user) {
        return new UserProfileDto(
                user.getId(),
                user.getSteamId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getProfileUrl(),
                user.getTradeUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    private SteamPlayerProfile defaultProfile(String steamId) {
        return new SteamPlayerProfile(
                defaultUsername(steamId),
                DEFAULT_AVATAR_URL,
                defaultProfileUrl(steamId)
        );
    }

    private String defaultUsername(String steamId) {
        String suffix = steamId.length() >= 6 ? steamId.substring(steamId.length() - 6) : steamId;
        return "Player_" + suffix;
    }

    private String defaultProfileUrl(String steamId) {
        return "https://steamcommunity.com/profiles/" + steamId;
    }

    public String getFrontendRedirectUrl() {
        return frontendRedirectUrl;
    }

    public boolean isDevModeEnabled() {
        return devModeEnabled;
    }

    private record SteamPlayerProfile(String username, String avatarUrl, String profileUrl) {}
}
