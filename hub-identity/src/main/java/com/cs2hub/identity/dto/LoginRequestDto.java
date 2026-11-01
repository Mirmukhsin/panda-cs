package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

@Schema(description = "Login request payload with username/email and password")
public record LoginRequestDto(
        @Schema(description = "User registered username or email", example = "player@cs2hub.com")
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @Schema(description = "Account password", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        String password
) implements Serializable {}
