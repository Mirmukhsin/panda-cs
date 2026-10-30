package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.LoadoutUpvoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LoadoutUpvoteRepository extends JpaRepository<LoadoutUpvoteEntity, UUID> {

    boolean existsByLoadoutIdAndUserId(UUID loadoutId, UUID userId);

    Optional<LoadoutUpvoteEntity> findByLoadoutIdAndUserId(UUID loadoutId, UUID userId);

    void deleteByLoadoutIdAndUserId(UUID loadoutId, UUID userId);

    long countByLoadoutId(UUID loadoutId);

    @Query("SELECT u.loadoutId FROM LoadoutUpvoteEntity u WHERE u.userId = :userId AND u.loadoutId IN :loadoutIds")
    Set<UUID> findUpvotedLoadoutIds(
            @Param("userId") UUID userId,
            @Param("loadoutIds") Collection<UUID> loadoutIds
    );
}
