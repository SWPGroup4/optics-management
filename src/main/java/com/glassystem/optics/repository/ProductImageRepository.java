package com.glassystem.optics.repository;


import com.glassystem.optics.entity.ProductImage;
import com.glassystem.optics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {}
