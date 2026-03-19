package com.glassystem.optics.specification;

import com.glassystem.optics.entity.Policy;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PolicySpecification {

	public static Specification<Policy> filter(
			String keyword,
			LocalDate effectiveFrom,
			LocalDate effectiveTo
	) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (keyword != null && !keyword.isBlank()) {
				String pattern = "%" + keyword.toLowerCase() + "%";
				Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
				Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
				predicates.add(cb.or(titleMatch, codeMatch));
			}

			if (effectiveFrom != null) {
				predicates.add(
						cb.greaterThanOrEqualTo(root.get("effectiveFrom"), effectiveFrom)
				);
			}

			if (effectiveTo != null) {
				predicates.add(
						cb.lessThanOrEqualTo(root.get("effectiveTo"), effectiveTo)
				);
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
