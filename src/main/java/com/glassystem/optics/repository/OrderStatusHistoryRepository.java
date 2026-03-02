package com.glassystem.optics.repository;


import com.glassystem.optics.entity.OrderStatusHistory;
import com.glassystem.optics.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {
    Optional<OrderStatusHistory> findTopByOrderIdOrderByChangedAtDesc(String orderId);
}
