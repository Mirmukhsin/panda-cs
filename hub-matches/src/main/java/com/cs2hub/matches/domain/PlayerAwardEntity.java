package com.cs2hub.matches.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "player_awards", schema = "matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAwardEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_player_id", nullable = false)
    private MatchPlayerEntity matchPlayer;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "badge_code", nullable = false, length = 64)
    private String badgeCode;

    @Column(name = "badge_title", nullable = false, length = 128)
    private String badgeTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_category", nullable = false, length = 16)
    private BadgeCategory badgeCategory;

    @Column(name = "badge_icon", nullable = false, length = 32)
    private String badgeIcon;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
