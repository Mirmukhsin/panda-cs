package com.cs2hub.identity.service;

import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAccountInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminAccountInitializer(userRepository, passwordEncoder);
        when(passwordEncoder.encode("ClutchAdmin#2026!")).thenReturn("$2a$10$encodedClutchAdminPassword");
    }

    @Test
    @DisplayName("Should demote non-clutch_admin admin accounts and create clutch_admin if absent")
    void shouldDemoteNonClutchAdminsAndCreateClutchAdmin() {
        UserEntity pandaAdmin = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("MR_Panda")
                .email("panda@example.com")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(new ArrayList<>(List.of(pandaAdmin)));
        when(userRepository.findByUsernameIgnoreCase("clutch_admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("admin@clutchlabs.gg")).thenReturn(Optional.empty());

        initializer.initializeDedicatedAdmin();

        // 1. MR_Panda should be demoted to USER
        assertThat(pandaAdmin.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(pandaAdmin);

        // 2. clutch_admin should be created with role ADMIN
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());

        List<UserEntity> savedUsers = captor.getAllValues();
        UserEntity createdAdmin = savedUsers.stream()
                .filter(u -> "clutch_admin".equals(u.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(createdAdmin.getUsername()).isEqualTo("clutch_admin");
        assertThat(createdAdmin.getEmail()).isEqualTo("admin@clutchlabs.gg");
        assertThat(createdAdmin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(createdAdmin.getPasswordHash()).isEqualTo("$2a$10$encodedClutchAdminPassword");
        assertThat(createdAdmin.getAvatarUrl()).isEqualTo(AuthService.DEFAULT_LOCAL_AVATAR_BASE + "clutch_admin");
    }

    @Test
    @DisplayName("Should update existing clutch_admin with standard admin attributes without demoting it")
    void shouldUpdateExistingClutchAdmin() {
        UserEntity existingAdmin = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("clutch_admin")
                .email("old@example.com")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(existingAdmin));
        when(userRepository.findByUsernameIgnoreCase("clutch_admin")).thenReturn(Optional.of(existingAdmin));

        initializer.initializeDedicatedAdmin();

        assertThat(existingAdmin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(existingAdmin.getEmail()).isEqualTo("admin@clutchlabs.gg");
        assertThat(existingAdmin.getPasswordHash()).isEqualTo("$2a$10$encodedClutchAdminPassword");
        assertThat(existingAdmin.getAvatarUrl()).isEqualTo(AuthService.DEFAULT_LOCAL_AVATAR_BASE + "clutch_admin");
        verify(userRepository).save(existingAdmin);
    }
}
