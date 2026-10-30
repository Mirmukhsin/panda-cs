package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.StickerEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class StickerSpecifications {

    private StickerSpecifications() {}

    public static Specification<StickerEntity> filter(
            SkinColor color,
            BigDecimal maxPrice,
            String query
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (color != null) {
                predicates.add(cb.equal(root.get("primaryColor"), color));
            }
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceUsd"), maxPrice));
            }
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
