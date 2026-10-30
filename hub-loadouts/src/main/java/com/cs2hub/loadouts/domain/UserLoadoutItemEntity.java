package com.cs2hub.loadouts.domain;

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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "user_loadout_items", schema = "loadouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoadoutItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loadout_id", nullable = false)
    private UserLoadoutEntity loadout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skin_id", nullable = false)
    private SkinEntity skin;

    @Column(name = "weapon_slot", nullable = false, length = 64)
    private String weaponSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "wear_condition", nullable = false, length = 16)
    private WearCondition wearCondition;

    @Column(name = "selected_price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal selectedPriceUsd;

    @Column(name = "stickers_json", columnDefinition = "TEXT")
    private String stickersJson;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
