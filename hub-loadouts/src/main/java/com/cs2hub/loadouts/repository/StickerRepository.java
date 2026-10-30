package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.StickerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StickerRepository extends JpaRepository<StickerEntity, String>, JpaSpecificationExecutor<StickerEntity> {

    List<StickerEntity> findAllByPrimaryColor(SkinColor color);
}
