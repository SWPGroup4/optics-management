package com.glassystem.optics.repository;

import com.glassystem.optics.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findByProductIdAndColorNameAndSizeLabel
            (String productId, String colorName, String sizeLabel);
}
