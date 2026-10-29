package com.cs2hub.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<AuthenticatedUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(AuthenticatedUser::userId);
    }

    public static boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(u -> role.equalsIgnoreCase(u.role()))
                .orElse(false);
    }
}
