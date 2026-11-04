package com.cs2hub.identity.service;

import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer {

    public static final String ADMIN_USERNAME = "clutch_admin";
    public static final String ADMIN_EMAIL = "admin@clutchlabs.gg";
    public static final String ADMIN_RAW_PASSWORD = "ClutchAdmin#2026!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        initializeDedicatedAdmin();
    }

    @Transactional
    public void initializeDedicatedAdmin() {
        log.info("Checking and isolating dedicated admin account...");

        // 1. Demote any existing user with role == UserRole.ADMIN whose username is NOT "clutch_admin"
        List<UserEntity> existingAdmins = userRepository.findByRole(UserRole.ADMIN);
        for (UserEntity admin : existingAdmins) {
            if (!ADMIN_USERNAME.equalsIgnoreCase(admin.getUsername())) {
                log.warn("Demoting user '{}' (id={}) from ADMIN to USER role", admin.getUsername(), admin.getId());
                admin.setRole(UserRole.USER);
                userRepository.save(admin);
            }
        }

        // 2. Upsert clutch_admin
        UserEntity adminUser = userRepository.findByUsernameIgnoreCase(ADMIN_USERNAME)
                .or(() -> userRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .orElseGet(() -> UserEntity.builder()
                        .id(UUID.randomUUID())
                        .build());

        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPasswordHash(passwordEncoder.encode(ADMIN_RAW_PASSWORD));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setAvatarUrl(AuthService.DEFAULT_LOCAL_AVATAR_BASE + ADMIN_USERNAME);

        userRepository.save(adminUser);
        log.info("Dedicated admin account '{}' successfully initialized with ADMIN role", ADMIN_USERNAME);
    }
}
