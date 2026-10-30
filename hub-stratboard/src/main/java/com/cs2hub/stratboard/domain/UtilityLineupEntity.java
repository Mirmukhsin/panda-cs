package com.cs2hub.stratboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utility_lineups", schema = "stratboard")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityLineupEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "map_name", nullable = false, length = 64)
    private String mapName;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 32)
    private UtilityType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 16)
    private StratSide teamSide;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "throw_type", nullable = false, length = 64)
    private ThrowType throwType;

    @Column(name = "stand_x", nullable = false)
    private Double standX;

    @Column(name = "stand_y", nullable = false)
    private Double standY;

    @Column(name = "land_x", nullable = false)
    private Double landX;

    @Column(name = "land_y", nullable = false)
    private Double landY;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.isVerified == null) {
            this.isVerified = true;
        }
    }
}
