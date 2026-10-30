package com.cs2hub.matches.repository;

import com.cs2hub.matches.domain.MatchAnalysisEntity;
import com.cs2hub.matches.domain.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchAnalysisRepository extends JpaRepository<MatchAnalysisEntity, UUID> {

    Optional<MatchAnalysisEntity> findByShareCode(String shareCode);

    Optional<MatchAnalysisEntity> findByShareSlug(String shareSlug);

    Optional<MatchAnalysisEntity> findFirstByStatusOrderByCreatedAtAsc(MatchStatus status);

    Page<MatchAnalysisEntity> findAllByStatusOrderByCompletedAtDesc(MatchStatus status, Pageable pageable);
}
