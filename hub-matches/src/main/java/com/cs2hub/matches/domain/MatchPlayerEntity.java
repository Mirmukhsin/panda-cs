package com.cs2hub.matches.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "match_players", schema = "matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchAnalysisEntity match;

    @Column(name = "steam_id", nullable = false, length = 32)
    private String steamId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "player_name", nullable = false, length = 128)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 16)
    private MatchTeamSide teamSide;

    @Builder.Default
    @Column(name = "kills", nullable = false)
    private Integer kills = 0;

    @Builder.Default
    @Column(name = "deaths", nullable = false)
    private Integer deaths = 0;

    @Builder.Default
    @Column(name = "assists", nullable = false)
    private Integer assists = 0;

    @Builder.Default
    @Column(name = "adr", nullable = false)
    private Double adr = 0.0;

    @Builder.Default
    @Column(name = "headshot_pct", nullable = false)
    private Double headshotPct = 0.0;

    @Builder.Default
    @Column(name = "kast_pct", nullable = false)
    private Double kastPct = 0.0;

    @Builder.Default
    @Column(name = "impact_rating", nullable = false)
    private Double impactRating = 1.0;

    @Builder.Default
    @Column(name = "first_kills", nullable = false)
    private Integer firstKills = 0;

    @Builder.Default
    @Column(name = "first_deaths", nullable = false)
    private Integer firstDeaths = 0;

    @Builder.Default
    @Column(name = "clutches_won", nullable = false)
    private Integer clutchesWon = 0;

    @Builder.Default
    @Column(name = "team_flashes", nullable = false)
    private Integer teamFlashes = 0;

    @Builder.Default
    @Column(name = "enemies_flashed", nullable = false)
    private Integer enemiesFlashed = 0;

    @Builder.Default
    @Column(name = "utility_damage", nullable = false)
    private Integer utilityDamage = 0;

    @Builder.Default
    @Column(name = "zero_damage_rounds", nullable = false)
    private Integer zeroDamageRounds = 0;

    @Builder.Default
    @Column(name = "eco_kills", nullable = false)
    private Integer ecoKills = 0;

    @Builder.Default
    @Column(name = "awp_kills", nullable = false)
    private Integer awpKills = 0;

    @Builder.Default
    @Column(name = "bomb_plants", nullable = false)
    private Integer bombPlants = 0;

    @Builder.Default
    @Column(name = "bomb_defuses", nullable = false)
    private Integer bombDefuses = 0;

    @Column(name = "headline_title", length = 128)
    private String headlineTitle;

    @Column(name = "headline_roast", columnDefinition = "TEXT")
    private String headlineRoast;

    @Builder.Default
    @OneToMany(mappedBy = "matchPlayer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerAwardEntity> awards = new ArrayList<>();

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
