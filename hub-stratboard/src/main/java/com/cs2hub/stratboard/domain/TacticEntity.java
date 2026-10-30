package com.cs2hub.stratboard.domain;

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
@Table(name = "tactics", schema = "stratboard")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TacticEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "map_name", nullable = false, length = 64)
    private String mapName;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 16)
    private StratSide teamSide;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "canvas_state_json", nullable = false, columnDefinition = "TEXT")
    private String canvasStateJson;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Builder.Default
    @Column(name = "upvotes", nullable = false)
    private Integer upvotes = 0;

    @Column(name = "share_code", nullable = false, unique = true, length = 32)
    private String shareCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isPublic == null) {
            this.isPublic = true;
        }
        if (this.upvotes == null) {
            this.upvotes = 0;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
