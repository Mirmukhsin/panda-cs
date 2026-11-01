package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.DailyChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallengeEntity, UUID> {

    Optional<DailyChallengeEntity> findByChallengeDate(LocalDate challengeDate);

    @Query("SELECT d FROM DailyChallengeEntity d LEFT JOIN FETCH d.challengeClips c LEFT JOIN FETCH c.clip WHERE d.challengeDate = :challengeDate")
    Optional<DailyChallengeEntity> findByChallengeDateWithClips(@Param("challengeDate") LocalDate challengeDate);
}
