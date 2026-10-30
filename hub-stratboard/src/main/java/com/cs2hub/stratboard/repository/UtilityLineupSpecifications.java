package com.cs2hub.stratboard.repository;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.UtilityLineupEntity;
import com.cs2hub.stratboard.domain.UtilityType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UtilityLineupSpecifications {

    private UtilityLineupSpecifications() {}

    public static Specification<UtilityLineupEntity> filter(
            String mapName,
            UtilityType utilityType,
            StratSide teamSide
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(mapName)) {
                predicates.add(cb.equal(root.get("mapName"), mapName.trim().toLowerCase()));
            }
            if (utilityType != null) {
                predicates.add(cb.equal(root.get("utilityType"), utilityType));
            }
            if (teamSide != null) {
                predicates.add(cb.equal(root.get("teamSide"), teamSide));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
