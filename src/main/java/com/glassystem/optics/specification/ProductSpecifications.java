package com.glassystem.optics.specification;

import java.math.BigDecimal;

import com.glassystem.optics.entity.Product;
import com.glassystem.optics.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecifications {
	private ProductSpecifications() {
	}

	public static Specification<Product> build(
			String q,
			String brand,
			String category,
			String frameType,
			String gender,
			String shape,
			String frameMaterial,
			String hingeType,
			String nosePadType,
			BigDecimal minWeightGram,
			BigDecimal maxWeightGram,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			ProductStatus status) {
		return (root, query, cb) -> {
			Predicate predicate = cb.conjunction();

			if (q != null && !q.isBlank()) {
				String like = "%" + q.trim().toLowerCase() + "%";
				Predicate name = cb.like(cb.lower(root.get("name")), like);
				Predicate brandLike = cb.like(cb.lower(root.get("brand")), like);
				Predicate categoryLike = cb.like(cb.lower(root.get("category")), like);
				Predicate frameTypeLike = cb.like(cb.lower(root.get("frameType")), like);
				Predicate genderLike = cb.like(cb.lower(root.get("gender")), like);
				Predicate shapeLike = cb.like(cb.lower(root.get("shape")), like);
				Predicate frameMaterialLike = cb.like(cb.lower(root.get("frameMaterial")), like);
				Predicate hingeTypeLike = cb.like(cb.lower(root.get("hingeType")), like);
				Predicate nosePadTypeLike = cb.like(cb.lower(root.get("nosePadType")), like);
				predicate = cb.and(
						predicate,
						cb.or(name, brandLike, categoryLike, frameTypeLike, genderLike, shapeLike, frameMaterialLike,
								hingeTypeLike, nosePadTypeLike));
			}

			if (brand != null && !brand.isBlank()) {
				String like = "%" + brand.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("brand")), like));
			}

			if (category != null && !category.isBlank()) {
				String like = "%" + category.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("category")), like));
			}

			if (frameType != null && !frameType.isBlank()) {
				String like = "%" + frameType.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("frameType")), like));
			}

			if (gender != null && !gender.isBlank()) {
				String like = "%" + gender.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("gender")), like));
			}

			if (shape != null && !shape.isBlank()) {
				String like = "%" + shape.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("shape")), like));
			}

			if (frameMaterial != null && !frameMaterial.isBlank()) {
				String like = "%" + frameMaterial.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("frameMaterial")), like));
			}

			if (hingeType != null && !hingeType.isBlank()) {
				String like = "%" + hingeType.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("hingeType")), like));
			}

			if (nosePadType != null && !nosePadType.isBlank()) {
				String like = "%" + nosePadType.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("nosePadType")), like));
			}

			if (minWeightGram != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("weightGram"), minWeightGram));
			}

			if (maxWeightGram != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("weightGram"), maxWeightGram));
			}

			if (minPrice != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
			}

			if (maxPrice != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
			}

			if (status != null) {
				predicate = cb.and(predicate, cb.equal(root.get("status"), status));
			}

			return predicate;
		};
	}
}
