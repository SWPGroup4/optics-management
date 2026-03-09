package com.glassystem.optics.repository;

import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String>, JpaSpecificationExecutor<ProductVariant> {

    java.util.Optional<ProductVariant> findByProductIdAndColorNameAndSizeLabel(String productId, String colorName, String sizeLabel);
    List<ProductVariant> findAllByIdAndStatus(String id, ProductVariantStatus status);

}