package com.cs2hub.loadouts.domain;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stickers", schema = "loadouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StickerEntity {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "rarity", length = 64)
    private String rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_color", length = 32)
    private SkinColor primaryColor;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (this.priceUsd == null) {
            this.priceUsd = BigDecimal.ZERO;
        }
        this.updatedAt = Instant.now();
    }
}
