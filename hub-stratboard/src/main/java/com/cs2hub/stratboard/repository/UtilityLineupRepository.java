package com.cs2hub.stratboard.repository;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UtilityLineupRepository extends JpaRepository<UtilityLineupEntity, UUID>, JpaSpecificationExecutor<UtilityLineupEntity> {

    @Query("SELECT l FROM UtilityLineupEntity l WHERE LOWER(REPLACE(l.mapName, 'de_', '')) = LOWER(REPLACE(:mapName, 'de_', ''))")
    List<UtilityLineupEntity> findByMapNameNormalized(@Param("mapName") String mapName);

    @Query("SELECT l FROM UtilityLineupEntity l WHERE LOWER(REPLACE(l.mapName, 'de_', '')) = LOWER(REPLACE(:mapName, 'de_', '')) AND l.utilityType = :utilityType")
    List<UtilityLineupEntity> findByMapNameNormalizedAndUtilityType(@Param("mapName") String mapName, @Param("utilityType") UtilityType utilityType);

    List<UtilityLineupEntity> findAllByMapName(String mapName);

    List<UtilityLineupEntity> findAllByMapNameAndUtilityType(String mapName, UtilityType utilityType);

    List<UtilityLineupEntity> findAllByMapNameAndTeamSide(String mapName, StratSide teamSide);

    List<UtilityLineupEntity> findAllByMapNameAndUtilityTypeAndTeamSide(String mapName, UtilityType utilityType, StratSide teamSide);
}
