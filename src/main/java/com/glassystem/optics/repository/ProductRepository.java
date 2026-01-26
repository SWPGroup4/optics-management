package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>  {
    Optional<Product> findByNameAndBrand (String name,String brand);
}
