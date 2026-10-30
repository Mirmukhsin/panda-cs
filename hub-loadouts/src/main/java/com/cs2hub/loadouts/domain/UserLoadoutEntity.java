package com.cs2hub.loadouts.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_loadouts", schema = "loadouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoadoutEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_color", length = 32)
    private SkinColor themeColor;

    @Column(name = "budget_limit_usd", precision = 10, scale = 2)
    private BigDecimal budgetLimitUsd;

    @Builder.Default
    @Column(name = "total_cost_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCostUsd = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "upvotes", nullable = false)
    private Integer upvotes = 0;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Builder.Default
    @OneToMany(mappedBy = "loadout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLoadoutItemEntity> items = new ArrayList<>();

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
        if (this.totalCostUsd == null) {
            this.totalCostUsd = BigDecimal.ZERO;
        }
        if (this.upvotes == null) {
            this.upvotes = 0;
        }
        if (this.isPublic == null) {
            this.isPublic = true;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
