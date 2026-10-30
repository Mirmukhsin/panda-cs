package com.cs2hub.matches.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "match_analyses", schema = "matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchAnalysisEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "share_code", nullable = false, unique = true, length = 128)
    private String shareCode;

    @Column(name = "demo_url", columnDefinition = "TEXT")
    private String demoUrl;

    @Column(name = "submitted_by_user_id")
    private UUID submittedByUserId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MatchStatus status = MatchStatus.QUEUED;

    @Column(name = "map_name", length = 64)
    private String mapName;

    @Builder.Default
    @Column(name = "team_a_score")
    private Integer teamAScore = 0;

    @Builder.Default
    @Column(name = "team_b_score")
    private Integer teamBScore = 0;

    @Builder.Default
    @Column(name = "total_rounds")
    private Integer totalRounds = 0;

    @Builder.Default
    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "share_slug", nullable = false, unique = true, length = 32)
    private String shareSlug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchPlayerEntity> players = new ArrayList<>();

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = MatchStatus.QUEUED;
        }
    }
}
