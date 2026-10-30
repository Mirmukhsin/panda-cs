package com.cs2hub.identity.service;

import com.cs2hub.common.spi.UserLookupService;
import com.cs2hub.common.spi.UserSummaryDto;
import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupServiceImpl implements UserLookupService {

    private final UserRepository userRepository;

    @Override
    public Optional<UserSummaryDto> findById(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(this::toSummaryDto);
    }

    @Override
    public Map<UUID, UserSummaryDto> findByIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllByIdIn(userIds).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toMap(
                        UserSummaryDto::userId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Optional<UserSummaryDto> findBySteamId(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findBySteamId(steamId.trim()).map(this::toSummaryDto);
    }

    private UserSummaryDto toSummaryDto(UserEntity user) {
        return new UserSummaryDto(
                user.getId(),
                user.getSteamId(),
                user.getUsername(),
                user.getAvatarUrl()
        );
    }
}
