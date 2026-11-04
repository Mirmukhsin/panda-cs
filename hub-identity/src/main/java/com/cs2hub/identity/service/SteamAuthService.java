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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
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
    private static final Pattern STEAM_XML_ID_PATTERN = Pattern.compile("<steamID>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</steamID>", Pattern.DOTALL);
    private static final Pattern STEAM_XML_AVATAR_PATTERN = Pattern.compile("<avatarFull>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</avatarFull>", Pattern.DOTALL);

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

        this.restClient = restClientBuilder != null
                ? restClientBuilder.build()
                : RestClient.builder()
                        .requestFactory(createDefaultRequestFactory())
                        .build();

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

        SteamProfileSummary playerProfile = fetchSteamProfileSummary(steamId);

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

        if (role == UserRole.ADMIN && !"clutch_admin".equalsIgnoreCase(request.username())) {
            log.warn("Non-clutch_admin user '{}' requested ADMIN role in dev login. Restricting to USER.", request.username());
            role = UserRole.USER;
        }

        final UserRole finalRole = role;
        UserEntity user = userRepository.findBySteamId(request.steamId())
                .or(() -> userRepository.findByUsernameIgnoreCase(request.username()))
                .map(existing -> {
                    existing.setUsername(request.username());
                    if (existing.getSteamId() == null && request.steamId() != null) {
                        existing.setSteamId(request.steamId());
                    }
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
            throw new UnauthorizedException("Could not verify OpenID ticket with Steam: " + ex.getMessage());
        }
    }

    public SteamProfileSummary fetchSteamProfileSummary(String steamId) {
        // Step A: Attempt Steam Web API if API key is provided
        if (StringUtils.hasText(apiKey)) {
            try {
                String apiUrl = UriComponentsBuilder.fromHttpUrl("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/")
                        .queryParam("key", apiKey)
                        .queryParam("steamids", steamId)
                        .toUriString();

                String jsonResponse = restClient.get()
                        .uri(apiUrl)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                if (StringUtils.hasText(jsonResponse)) {
                    JsonNode root = objectMapper.readTree(jsonResponse);
                    JsonNode players = root.path("response").path("players");
                    if (players.isArray() && !players.isEmpty()) {
                        JsonNode p = players.get(0);
                        String personaname = p.path("personaname").asText(defaultUsername(steamId));
                        String avatarfull = p.path("avatarfull").asText(DEFAULT_AVATAR_URL);
                        String profileurl = p.path("profileurl").asText(defaultProfileUrl(steamId));
                        log.info("Resolved real Steam profile for {} via Web API: nickname='{}'", steamId, personaname);
                        return new SteamProfileSummary(personaname, avatarfull, profileurl);
                    }
                }
            } catch (Exception ex) {
                log.warn("Step A (Steam Web API) failed for steamId {}: {}", steamId, ex.getMessage());
            }
        }

        // Step B: Public XML Fallback: https://steamcommunity.com/profiles/{steamId}/?xml=1
        try {
            String xmlUrl = "https://steamcommunity.com/profiles/" + steamId + "/?xml=1";
            String responseXml = restClient.get()
                    .uri(xmlUrl)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL)
                    .retrieve()
                    .body(String.class);

            if (StringUtils.hasText(responseXml) && !responseXml.contains("<error>")) {
                String nickname = extractTagContent(STEAM_XML_ID_PATTERN, responseXml);
                String avatarFull = extractTagContent(STEAM_XML_AVATAR_PATTERN, responseXml);

                if (StringUtils.hasText(nickname)) {
                    log.info("Resolved real Steam profile for {} via XML fallback: nickname='{}'", steamId, nickname);
                    return new SteamProfileSummary(
                            nickname,
                            StringUtils.hasText(avatarFull) ? avatarFull : DEFAULT_AVATAR_URL,
                            defaultProfileUrl(steamId)
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Step B (Steam Community XML fallback) failed for steamId {}: {}", steamId, ex.getMessage());
        }

        return defaultProfile(steamId);
    }

    public String extractTagContent(Pattern pattern, String text) {
        if (text == null) return null;
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String content = m.group(1);
            if (content != null) {
                content = content.trim();
                if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
                    content = content.substring(9, content.length() - 3).trim();
                }
                return content.replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'");
            }
        }
        return null;
    }

    private UserEntity upsertUser(String steamId, SteamProfileSummary profile) {
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
                user.getEmail(),
                user.getAvatarUrl(),
                user.getProfileUrl(),
                user.getTradeUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    public SteamProfileSummary defaultProfile(String steamId) {
        return new SteamProfileSummary(
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

    private static SimpleClientHttpRequestFactory createDefaultRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return requestFactory;
    }

    public record SteamProfileSummary(String username, String avatarUrl, String profileUrl) {}
}
