package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Product;
import com.glassystem.optics.enums.ProductCategory;
import com.glassystem.optics.enums.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategoryAndGenderAndIdNotAndStatus(
            ProductCategory category,
            String gender,
            String id,
            ProductStatus status,
            Pageable pageable);
}
