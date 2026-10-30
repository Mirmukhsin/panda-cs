package com.cs2hub.minigames.repository;

import com.cs2hub.minigames.domain.ClipEntity;
import com.cs2hub.minigames.domain.ClipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClipRepository extends JpaRepository<ClipEntity, UUID> {

    List<ClipEntity> findAllBySubmittedByUserIdOrderByCreatedAtDesc(UUID submittedByUserId);

    List<ClipEntity> findAllByStatusOrderByCreatedAtDesc(ClipStatus status);

    List<ClipEntity> findTop3ByStatusOrderByCreatedAtAsc(ClipStatus status);

    long countByStatus(ClipStatus status);
}
