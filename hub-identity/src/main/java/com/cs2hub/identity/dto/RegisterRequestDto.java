package com.cs2hub.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

@Schema(description = "Registration request payload with username, email, and password")
public record RegisterRequestDto(
        @Schema(description = "User display / account name", example = "s1mple_pro")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
        String username,

        @Schema(description = "User contact email", example = "player@cs2hub.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Account password (at least 6 characters)", example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 128, message = "Password must be at least 6 characters")
        String password
) implements Serializable {}
