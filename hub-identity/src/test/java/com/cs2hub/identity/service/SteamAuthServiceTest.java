package com.cs2hub.identity.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.JwtTokenProvider;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.dto.AuthTokenResponseDto;
import com.cs2hub.identity.dto.DevLoginRequestDto;
import com.cs2hub.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class SteamAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtTokenProvider jwtTokenProvider;
    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private SteamAuthService steamAuthService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                604800000L
        );
        objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        steamAuthService = new SteamAuthService(
                userRepository,
                jwtTokenProvider,
                builder,
                objectMapper,
                "TEST_STEAM_API_KEY",
                "http://localhost:8080",
                "http://localhost:8080/api/v1/auth/steam/callback",
                "http://localhost:3000/auth/callback",
                true
        );
    }

    @Test
    @DisplayName("Should build valid Steam OpenID 2.0 login redirect URL")
    void shouldBuildLoginRedirectUrl() {
        String url = steamAuthService.buildLoginRedirectUrl();

        assertThat(url).startsWith("https://steamcommunity.com/openid/login");
        assertThat(url).contains("openid.ns=http://specs.openid.net/auth/2.0");
        assertThat(url).contains("openid.mode=checkid_setup");
        assertThat(url).contains("openid.return_to=http://localhost:8080/api/v1/auth/steam/callback");
        assertThat(url).contains("openid.realm=http://localhost:8080");
        assertThat(url).contains("openid.identity=http://specs.openid.net/auth/2.0/identifier_select");
        assertThat(url).contains("openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select");
    }

    @Test
    @DisplayName("Should execute dev login, upsert user and issue valid JWT")
    void shouldExecuteDevLogin() {
        DevLoginRequestDto request = new DevLoginRequestDto(
                "76561198012345678",
                "m0NESY",
                "ADMIN"
        );

        when(userRepository.findBySteamId("76561198012345678")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });

        AuthTokenResponseDto response = steamAuthService.devLogin(request);

        assertThat(response).isNotNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
        assertThat(response.user().steamId()).isEqualTo("76561198012345678");
        assertThat(response.user().username()).isEqualTo("m0NESY");
        assertThat(response.user().role()).isEqualTo("ADMIN");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should fail dev login when devModeEnabled is false")
    void shouldFailDevLoginWhenDisabled() {
        SteamAuthService disabledAuthService = new SteamAuthService(
                userRepository,
                jwtTokenProvider,
                null,
                objectMapper,
                "",
                "http://localhost:8080",
                "http://localhost:8080/api/v1/auth/steam/callback",
                "http://localhost:3000/auth/callback",
                false
        );

        DevLoginRequestDto request = new DevLoginRequestDto("76561198012345678", "dev", "USER");

        assertThatThrownBy(() -> disabledAuthService.devLogin(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("Should verify OpenID response and authenticate user with Steam Web API details")
    void shouldVerifyAndAuthenticateWithSteamWebAPI() {
        String steamId = "76561198087654321";
        Map<String, String> queryParams = Map.of(
                "openid.ns", "http://specs.openid.net/auth/2.0",
                "openid.mode", "id_res",
                "openid.claimed_id", "https://steamcommunity.com/openid/id/" + steamId,
                "openid.identity", "https://steamcommunity.com/openid/id/" + steamId,
                "openid.return_to", "http://localhost:8080/api/v1/auth/steam/callback"
        );

        // Mock OpenID validation response
        mockServer.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n", MediaType.TEXT_PLAIN));

        // Mock Steam Web API player summary response
        String steamApiResponse = """
                {
                  "response": {
                    "players": [
                      {
                        "steamid": "76561198087654321",
                        "personaname": "b1t",
                        "profileurl": "https://steamcommunity.com/id/b1t_cs",
                        "avatarfull": "https://avatars.steamstatic.com/b1t_avatar.jpg"
                      }
                    ]
                  }
                }
                """;
        mockServer.expect(requestTo("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=TEST_STEAM_API_KEY&steamids=" + steamId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(steamApiResponse, MediaType.APPLICATION_JSON));

        when(userRepository.findBySteamId(steamId)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });

        AuthTokenResponseDto response = steamAuthService.verifyAndAuthenticate(queryParams);

        mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.user().username()).isEqualTo("b1t");
        assertThat(response.user().avatarUrl()).isEqualTo("https://avatars.steamstatic.com/b1t_avatar.jpg");
        assertThat(response.user().profileUrl()).isEqualTo("https://steamcommunity.com/id/b1t_cs");
        assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid claimed_id format")
    void shouldRejectInvalidClaimedId() {
        Map<String, String> queryParams = Map.of(
                "openid.claimed_id", "https://invalid-site.com/id/12345"
        );

        assertThatThrownBy(() -> steamAuthService.verifyAndAuthenticate(queryParams))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid Steam claimed_id format");
    }

    @Test
    @DisplayName("Should reject when OpenID verification response is invalid")
    void shouldRejectInvalidOpenIdVerification() {
        String steamId = "76561198011223344";
        Map<String, String> queryParams = Map.of(
                "openid.claimed_id", "https://steamcommunity.com/openid/id/" + steamId
        );

        mockServer.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("is_valid:false\n", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> steamAuthService.verifyAndAuthenticate(queryParams))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Steam OpenID verification failed");

        mockServer.verify();
    }
}
