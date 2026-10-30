package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.WeaponCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkinRepository extends JpaRepository<SkinEntity, String>, JpaSpecificationExecutor<SkinEntity> {

    List<SkinEntity> findAllByPrimaryColor(SkinColor color);

    List<SkinEntity> findAllByWeaponId(String weaponId);

    List<SkinEntity> findAllByCategory(WeaponCategory category);
}
