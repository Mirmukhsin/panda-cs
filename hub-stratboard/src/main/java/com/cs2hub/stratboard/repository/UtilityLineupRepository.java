package com.cs2hub.stratboard.repository;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UtilityLineupRepository extends JpaRepository<UtilityLineupEntity, UUID>, JpaSpecificationExecutor<UtilityLineupEntity> {

    List<UtilityLineupEntity> findAllByMapName(String mapName);

    List<UtilityLineupEntity> findAllByMapNameAndUtilityType(String mapName, UtilityType utilityType);

    List<UtilityLineupEntity> findAllByMapNameAndTeamSide(String mapName, StratSide teamSide);

    List<UtilityLineupEntity> findAllByMapNameAndUtilityTypeAndTeamSide(String mapName, UtilityType utilityType, StratSide teamSide);
}
