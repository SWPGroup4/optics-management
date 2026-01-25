package com.glassystem.optics.specification;

import java.math.BigDecimal;

import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.ProductVariantStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ProductVariantSpecifications {
	private ProductVariantSpecifications() {}

	public static Specification<ProductVariant> build(
			String q,
			String productId,
			String colorName,
			String frameFinish,
			String sizeLabel,
			Integer lensWidthMm,
			Integer bridgeWidthMm,
			Integer templeLengthMm,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			ProductVariantStatus status) {
		return (root, query, cb) -> {
			Predicate predicate = cb.conjunction();

			if (q != null && !q.isBlank()) {
				String like = "%" + q.trim().toLowerCase() + "%";
				Predicate colorLike = cb.like(cb.lower(root.get("colorName")), like);
				Predicate finishLike = cb.like(cb.lower(root.get("frameFinish")), like);
				Predicate sizeLike = cb.like(cb.lower(root.get("sizeLabel")), like);
				predicate = cb.and(predicate, cb.or(colorLike, finishLike, sizeLike));
			}

			if (productId != null) {
				predicate = cb.and(predicate, cb.equal(root.get("product").get("id"), productId));
			}

			if (colorName != null && !colorName.isBlank()) {
				String like = "%" + colorName.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("colorName")), like));
			}

			if (frameFinish != null && !frameFinish.isBlank()) {
				String like = "%" + frameFinish.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("frameFinish")), like));
			}

			if (sizeLabel != null && !sizeLabel.isBlank()) {
				String like = "%" + sizeLabel.trim().toLowerCase() + "%";
				predicate = cb.and(predicate, cb.like(cb.lower(root.get("sizeLabel")), like));
			}

			if (lensWidthMm != null) {
				predicate = cb.and(predicate, cb.equal(root.get("lensWidthMm"), lensWidthMm));
			}

			if (bridgeWidthMm != null) {
				predicate = cb.and(predicate, cb.equal(root.get("bridgeWidthMm"), bridgeWidthMm));
			}

			if (templeLengthMm != null) {
				predicate = cb.and(predicate, cb.equal(root.get("templeLengthMm"), templeLengthMm));
			}

			if (minPrice != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
			}

			if (maxPrice != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
			}

			if (status != null) {
				predicate = cb.and(predicate, cb.equal(root.get("status"), status));
			}

			return predicate;
		};
	}
}
