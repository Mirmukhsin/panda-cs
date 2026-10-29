package com.cs2hub.identity.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.identity.dto.AuthTokenResponseDto;
import com.cs2hub.identity.dto.DevLoginRequestDto;
import com.cs2hub.identity.service.SteamAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@Tag(name = "Authentication", description = "Steam OpenID 2.0 authentication and local development credentials")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SteamAuthService steamAuthService;

    @Operation(
            summary = "Initiate Steam OpenID 2.0 Login",
            description = "Redirects browser to Steam Community OpenID login or returns the generated login URL in JSON."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to Steam OpenID login portal"),
            @ApiResponse(responseCode = "200", description = "Login URL returned as JSON (when redirect=false)")
    })
    @GetMapping("/steam/login")
    public ResponseEntity<?> steamLogin(
            @Parameter(description = "If true, issues a 302 redirect; if false, returns JSON with loginUrl")
            @RequestParam(required = false, defaultValue = "true") boolean redirect
    ) {
        String loginUrl = steamAuthService.buildLoginRedirectUrl();

        if (redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(loginUrl))
                    .build();
        }

        return ResponseEntity.ok(ApiResponseDto.ok("Steam login URL generated", Map.of("loginUrl", loginUrl)));
    }

    @Operation(
            summary = "Steam OpenID 2.0 Callback",
            description = "Validates the OpenID response returned by Steam, upserts user profile, and redirects to frontend or returns JWT in JSON."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to frontend callback URL with token query parameter"),
            @ApiResponse(responseCode = "200", description = "Authentication successful, returns AuthTokenResponseDto (when format=json)"),
            @ApiResponse(responseCode = "400", description = "Invalid OpenID response parameters"),
            @ApiResponse(responseCode = "401", description = "OpenID verification failed by Steam")
    })
    @GetMapping("/steam/callback")
    public ResponseEntity<?> steamCallback(
            @Parameter(hidden = true)
            @RequestParam Map<String, String> queryParams,
            @Parameter(description = "Response format ('redirect' to frontend or 'json' for raw token payload)")
            @RequestParam(required = false, defaultValue = "redirect") String format
    ) {
        AuthTokenResponseDto tokenResponse = steamAuthService.verifyAndAuthenticate(queryParams);

        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(ApiResponseDto.ok("Steam authentication successful", tokenResponse));
        }

        String targetRedirect = steamAuthService.getFrontendRedirectUrl() + "?token=" + tokenResponse.accessToken();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetRedirect))
                .build();
    }

    @Operation(
            summary = "Development Fast-Login Helper",
            description = "Available in local and dev environments (app.auth.dev-mode-enabled=true). Immediately issues a valid JWT for testing."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dev authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Dev login is disabled in this environment")
    })
    @PostMapping("/dev-login")
    public ResponseEntity<ApiResponseDto<AuthTokenResponseDto>> devLogin(
            @Valid @RequestBody DevLoginRequestDto request
    ) {
        AuthTokenResponseDto tokenResponse = steamAuthService.devLogin(request);
        return ResponseEntity.ok(ApiResponseDto.ok("Dev login successful", tokenResponse));
    }
}
