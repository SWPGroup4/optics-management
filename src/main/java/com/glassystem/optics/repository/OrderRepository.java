package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, String> {
    List<Orders> findByCustomerId(String customerId);
    List<Orders> findByStatus(OrderStatus status);
    List<Orders> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.createdAt = :date")
    long countByCreatedAt(@Param("date") LocalDate date);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
}
