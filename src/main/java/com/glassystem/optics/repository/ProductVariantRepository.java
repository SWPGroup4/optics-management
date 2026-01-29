package com.glassystem.optics.repository;

import com.glassystem.optics.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String>, JpaSpecificationExecutor<ProductVariant> {}
