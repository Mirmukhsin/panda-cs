package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.DailyChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallengeEntity, UUID> {

    Optional<DailyChallengeEntity> findByChallengeDate(LocalDate challengeDate);
}
