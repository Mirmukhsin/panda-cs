package com.cs2hub.stratboard.repository;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.TacticEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TacticRepository extends JpaRepository<TacticEntity, UUID>, JpaSpecificationExecutor<TacticEntity> {

    Optional<TacticEntity> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);

    List<TacticEntity> findAllByAuthorUserIdOrderByCreatedAtDesc(UUID authorUserId);

    Page<TacticEntity> findAllByMapNameAndIsPublicTrueOrderByUpvotesDesc(String mapName, Pageable pageable);

    Page<TacticEntity> findAllByMapNameAndTeamSideAndIsPublicTrueOrderByUpvotesDesc(String mapName, StratSide teamSide, Pageable pageable);
}
