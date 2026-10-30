package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SkinSpecifications {

    private SkinSpecifications() {}

    public static Specification<SkinEntity> filter(
            SkinColor color,
            String weaponId,
            WeaponCategory category,
            TeamSide teamSide,
            BigDecimal maxPrice,
            String query
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (color != null) {
                predicates.add(cb.equal(root.get("primaryColor"), color));
            }
            if (StringUtils.hasText(weaponId)) {
                predicates.add(cb.equal(root.get("weaponId"), weaponId.trim().toLowerCase()));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (teamSide != null && teamSide != TeamSide.BOTH) {
                predicates.add(root.get("teamSide").in(teamSide, TeamSide.BOTH));
            }
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lowestPriceUsd"), maxPrice));
            }
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
