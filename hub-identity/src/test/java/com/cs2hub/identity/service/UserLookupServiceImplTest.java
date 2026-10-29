package com.cs2hub.identity.service;

import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import com.cs2hub.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserLookupServiceImpl userLookupService;

    @BeforeEach
    void setUp() {
        userLookupService = new UserLookupServiceImpl(userRepository);
    }

    @Test
    @DisplayName("Should find user by id and map to UserSummaryDto")
    void shouldFindById() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .steamId("76561198000000001")
                .username("s1mple")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<UserSummaryDto> result = userLookupService.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().steamId()).isEqualTo("76561198000000001");
        assertThat(result.get().username()).isEqualTo("s1mple");
        assertThat(result.get().avatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("Should return empty optional when user id is null or not found")
    void shouldReturnEmptyWhenNotFound() {
        assertThat(userLookupService.findById(null)).isEmpty();

        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
        assertThat(userLookupService.findById(unknownId)).isEmpty();
    }

    @Test
    @DisplayName("Should batch lookup users by IDs and return Map of UserSummaryDto")
    void shouldBatchLookupByIds() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        UserEntity user1 = UserEntity.builder()
                .id(user1Id)
                .steamId("76561198000000001")
                .username("ZywOo")
                .avatarUrl("https://example.com/zywoo.jpg")
                .role(UserRole.USER)
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(user2Id)
                .steamId("76561198000000002")
                .username("NiKo")
                .avatarUrl("https://example.com/niko.jpg")
                .role(UserRole.USER)
                .build();

        when(userRepository.findAllByIdIn(Set.of(user1Id, user2Id))).thenReturn(List.of(user1, user2));

        Map<UUID, UserSummaryDto> result = userLookupService.findByIds(Set.of(user1Id, user2Id));

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(user1Id, user2Id);
        assertThat(result.get(user1Id).username()).isEqualTo("ZywOo");
        assertThat(result.get(user2Id).username()).isEqualTo("NiKo");
    }

    @Test
    @DisplayName("Should return empty map when batch lookup has empty or null set")
    void shouldReturnEmptyMapForEmptySet() {
        assertThat(userLookupService.findByIds(null)).isEmpty();
        assertThat(userLookupService.findByIds(Collections.emptySet())).isEmpty();
    }
}
