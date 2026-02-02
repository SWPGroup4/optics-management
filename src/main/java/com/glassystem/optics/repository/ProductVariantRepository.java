package com.glassystem.optics.repository;

import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.ProductVariantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String>, JpaSpecificationExecutor<ProductVariant> {

    java.util.Optional<ProductVariant> findByProduct_IdAndColorNameAndSizeLabel(String productId, String colorName, String sizeLabel);

    List<ProductVariant> findAllByProduct_IdAndStatus(String productId, ProductVariantStatus status);

    List<ProductVariant> findAllByIdIn(List<String> ids);

    @Query("SELECT v FROM ProductVariant v " +
            "WHERE v.status = :status " +
            "AND (:productId IS NULL OR v.product.id = :productId) " +
            "ORDER BY (" +
            "ABS(COALESCE(v.lensWidthMm, 0) - :lensWidthMm) + " +
            "ABS(COALESCE(v.bridgeWidthMm, 0) - :bridgeWidthMm) + " +
            "ABS(COALESCE(v.templeLengthMm, 0) - :templeLengthMm)" +
            ") ASC")
    List<ProductVariant> findNearestBySize(
            @Param("lensWidthMm") Integer lensWidthMm,
            @Param("bridgeWidthMm") Integer bridgeWidthMm,
            @Param("templeLengthMm") Integer templeLengthMm,
            @Param("productId") String productId,
            @Param("status") ProductVariantStatus status,
            Pageable pageable);
}