package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.UserLoadoutEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLoadoutRepository extends JpaRepository<UserLoadoutEntity, UUID>, JpaSpecificationExecutor<UserLoadoutEntity> {

    List<UserLoadoutEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<UserLoadoutEntity> findAllByIsPublicTrueOrderByUpvotesDesc(Pageable pageable);

    Page<UserLoadoutEntity> findAllByThemeColorAndIsPublicTrueOrderByUpvotesDesc(SkinColor themeColor, Pageable pageable);
}
