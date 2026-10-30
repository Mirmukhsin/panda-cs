package com.cs2hub.loadouts.repository;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.UserLoadoutEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class UserLoadoutSpecifications {

    private UserLoadoutSpecifications() {}

    public static Specification<UserLoadoutEntity> filterPublic(SkinColor color, BigDecimal maxBudget) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always only public loadouts
            predicates.add(cb.isTrue(root.get("isPublic")));

            if (color != null) {
                predicates.add(cb.equal(root.get("themeColor"), color));
            }
            if (maxBudget != null && maxBudget.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalCostUsd"), maxBudget));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
