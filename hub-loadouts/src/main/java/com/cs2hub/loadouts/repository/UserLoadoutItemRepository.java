package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.UserLoadoutItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLoadoutItemRepository extends JpaRepository<UserLoadoutItemEntity, UUID> {

    List<UserLoadoutItemEntity> findAllByLoadoutId(UUID loadoutId);
}
