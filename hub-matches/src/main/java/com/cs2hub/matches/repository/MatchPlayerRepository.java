package com.cs2hub.matches.repository;

import com.cs2hub.matches.domain.MatchPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayerEntity, UUID> {

    Optional<MatchPlayerEntity> findByMatchIdAndSteamId(UUID matchId, String steamId);

    List<MatchPlayerEntity> findAllBySteamId(String steamId);
}
