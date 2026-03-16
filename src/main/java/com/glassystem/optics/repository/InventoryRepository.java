package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductVariantId(String productVariantId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity <= :threshold AND i.quantity > 0")
    long countLowStockItems(@Param("threshold") int threshold);
}