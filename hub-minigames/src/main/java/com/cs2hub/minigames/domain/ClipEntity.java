package com.cs2hub.minigames.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clips", schema = "minigames")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClipEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "submitted_by_user_id")
    private UUID submittedByUserId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_provider", nullable = false, length = 32)
    private VideoProvider videoProvider;

    @Column(name = "map_name", nullable = false, length = 64)
    private String mapName;

    @Column(name = "actual_premier_rating", nullable = false)
    private Integer actualPremierRating;

    @Column(name = "actual_faceit_level")
    private Integer actualFaceitLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ClipStatus status;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Builder.Default
    @Column(name = "is_curated", nullable = false)
    private Boolean isCurated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = ClipStatus.PENDING;
        }
        if (this.isCurated == null) {
            this.isCurated = false;
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
