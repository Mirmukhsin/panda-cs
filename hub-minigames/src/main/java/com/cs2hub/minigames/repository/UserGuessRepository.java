package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.UserGuessEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGuessRepository extends JpaRepository<UserGuessEntity, UUID> {

    List<UserGuessEntity> findAllByChallengeIdAndUserId(UUID challengeId, UUID userId);

    List<UserGuessEntity> findAllByChallengeIdAndGuestSessionId(UUID challengeId, String guestSessionId);

    Optional<UserGuessEntity> findByChallengeIdAndClipIdAndUserId(UUID challengeId, UUID clipId, UUID userId);

    Optional<UserGuessEntity> findByChallengeIdAndClipIdAndGuestSessionId(UUID challengeId, UUID clipId, String guestSessionId);

    @Query("""
        SELECT g.userId AS userId, SUM(g.pointsAwarded) AS totalScore, COUNT(g.id) AS roundsCount
        FROM UserGuessEntity g
        WHERE g.challenge.id = :challengeId AND g.userId IS NOT NULL
        GROUP BY g.userId
        ORDER BY SUM(g.pointsAwarded) DESC
    """)
    List<LeaderboardRowProjection> findDailyLeaderboard(@Param("challengeId") UUID challengeId, Pageable pageable);

    @Query("""
        SELECT g.userId AS userId, SUM(g.pointsAwarded) AS totalScore, COUNT(g.id) AS roundsCount
        FROM UserGuessEntity g
        WHERE g.userId IS NOT NULL
        GROUP BY g.userId
        ORDER BY SUM(g.pointsAwarded) DESC
    """)
    List<LeaderboardRowProjection> findAllTimeLeaderboard(Pageable pageable);
}
