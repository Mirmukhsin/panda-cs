package com.cs2hub.common.spi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service Provider Interface (SPI) for cross-module user profile lookups.
 * Implemented by hub-identity so other modules (minigames, loadouts, matches, stratboard)
 * can query public user profiles without cross-module JPA repository imports or SQL joins.
 */
public interface UserLookupService {

    /**
     * Look up a single user by their unique identifier.
     *
     * @param userId the unique identifier of the user
     * @return Optional containing UserSummaryDto if found, empty otherwise
     */
    Optional<UserSummaryDto> findById(UUID userId);

    /**
     * Batch look up multiple users by their unique identifiers.
     *
     * @param userIds set of user UUIDs to resolve
     * @return map of UUID to UserSummaryDto for all resolved users
     */
    Map<UUID, UserSummaryDto> findByIds(Set<UUID> userIds);

    /**
     * Look up a single user by their 64-bit Steam ID.
     *
     * @param steamId the 64-bit Steam identifier
     * @return Optional containing UserSummaryDto if found, empty otherwise
     */
    default Optional<UserSummaryDto> findBySteamId(String steamId) {
        return Optional.empty();
    }
}
