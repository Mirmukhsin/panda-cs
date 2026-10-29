package com.cs2hub.identity.repository;

import com.cs2hub.identity.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySteamId(String steamId);

    List<UserEntity> findAllByIdIn(Collection<UUID> ids);
}
