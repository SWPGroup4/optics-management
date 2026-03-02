package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = {"variants"})
    Optional<Product> findById(String id);

    @Override
    @EntityGraph(attributePaths = {"variants"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"variants"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.imageUrl")
    List<Product> findAllWithImages();
}
