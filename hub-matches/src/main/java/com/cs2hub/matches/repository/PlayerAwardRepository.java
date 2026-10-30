package com.cs2hub.matches.repository;

import com.cs2hub.matches.domain.PlayerAwardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerAwardRepository extends JpaRepository<PlayerAwardEntity, UUID> {

    List<PlayerAwardEntity> findAllByMatchPlayerId(UUID matchPlayerId);

    List<PlayerAwardEntity> findAllByMatchId(UUID matchId);
}
