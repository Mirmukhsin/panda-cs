package com.cs2hub.stratboard.repository;

import com.cs2hub.stratboard.domain.StratSide;
import com.cs2hub.stratboard.domain.TacticEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TacticSpecifications {

    private TacticSpecifications() {}

    public static Specification<TacticEntity> filterPublic(String mapName, StratSide teamSide) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isPublic")));

            if (StringUtils.hasText(mapName)) {
                predicates.add(cb.equal(root.get("mapName"), mapName.trim().toLowerCase()));
            }
            if (teamSide != null) {
                predicates.add(cb.equal(root.get("teamSide"), teamSide));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
