package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.DailyChallengeClipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyChallengeClipRepository extends JpaRepository<DailyChallengeClipEntity, UUID> {

    List<DailyChallengeClipEntity> findAllByChallengeIdOrderByRoundOrderAsc(UUID challengeId);
}
