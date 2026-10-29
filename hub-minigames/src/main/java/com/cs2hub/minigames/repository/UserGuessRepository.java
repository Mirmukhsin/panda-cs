package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.UserGuessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGuessRepository extends JpaRepository<UserGuessEntity, UUID> {

    List<UserGuessEntity> findAllByChallengeIdAndUserId(UUID challengeId, UUID userId);

    Optional<UserGuessEntity> findByChallengeIdAndClipIdAndUserId(UUID challengeId, UUID clipId, UUID userId);
}
