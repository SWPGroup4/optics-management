package com.glassystem.optics.specification;

import java.math.BigDecimal;
import java.time.Instant;

import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductCategory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecifications {
	private ProductSpecifications() {}

	public static Specification<Product> build(
			String q,
			String brand,
			ProductCategory category,
			Boolean isPrescriptionRequired,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Instant createdFrom,
			Instant createdTo) {
		return (root, query, cb) -> {
			Predicate predicate = cb.conjunction();

			if (q != null && !q.isBlank()) {
				String like = "%" + q.trim().toLowerCase() + "%";
				Predicate name = cb.like(cb.lower(root.get("name")), like);
				Predicate brandLike = cb.like(cb.lower(root.get("brand")), like);
				Predicate description = cb.like(cb.lower(root.get("description")), like);
				predicate = cb.and(predicate, cb.or(name, brandLike, description));
			}

			if (brand != null && !brand.isBlank()) {
				String like = "%" + brand.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("brand")), like));
			}

			if (category != null) {
				predicate = cb.and(predicate, cb.equal(root.get("category"), category));
			}

			if (isPrescriptionRequired != null) {
				predicate = cb.and(predicate, cb.equal(root.get("isPrescriptionRequired"), isPrescriptionRequired));
			}

			if (minPrice != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
			}

			if (maxPrice != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
			}

			if (createdFrom != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
			}

			if (createdTo != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
			}

			return predicate;
		};
	}
}
