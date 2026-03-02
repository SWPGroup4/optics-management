package com.glassystem.optics.specification;

import com.glassystem.optics.entity.Combo;
import com.glassystem.optics.enums.ComboStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ComboSpecification {

    public static Specification<Combo> filter(
            ComboStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (fromDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("startTime"), fromDate)
                );
            }

            if (toDate != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("endTime"), toDate)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}