package com.cs2hub.identity.repository;

import com.cs2hub.identity.domain.UserEntity;
import com.cs2hub.identity.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySteamId(String steamId);

    List<UserEntity> findAllByIdIn(Collection<UUID> ids);

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findByRole(UserRole role);

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) = LOWER(:usernameOrEmail) OR (u.email IS NOT NULL AND LOWER(u.email) = LOWER(:usernameOrEmail))")
    Optional<UserEntity> findByUsernameOrEmailIgnoreCase(@Param("usernameOrEmail") String usernameOrEmail);
}
