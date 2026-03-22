package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.PaymentStatus;
import com.glassystem.optics.enums.PreOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, String> {
    List<Orders> findByCustomerId(String customerId);
    Page<Orders> findByCustomerId(String customerId, Pageable pageable);
    List<Orders> findByStatus(OrderStatus status);
    Page<Orders> findByStatus(OrderStatus status, Pageable pageable);
    List<Orders> findByCustomerIdAndStatus(String customerId, OrderStatus status);
    Page<Orders> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);
    List<Orders> findByShipperIdAndStatus(String shipperId, OrderStatus status);
    Page<Orders> findByShipperIdAndStatus(String shipperId, OrderStatus status, Pageable pageable);


    @Query("SELECT o FROM Orders o WHERE o.status = :status AND EXISTS (" +
            "SELECT 1 FROM Payment p WHERE p.order = o AND p.status = :paymentStatus)")
    Page<Orders> findByStatusAndPaymentStatus(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable);


    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.createdAt = :date")
    long countByCreatedAt(@Param("date") LocalDate date);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("""
            SELECT DISTINCT o
            FROM Orders o
            JOIN o.items i
            WHERE i.productVariant.id = :variantId
              AND i.orderItemType = :orderItemType
              AND o.preOrderStatus = :preOrderStatus
              AND o.status = :orderStatus
            ORDER BY o.createdAt ASC
            """)
    List<Orders> findEligiblePreOrdersForVariant(
            @Param("variantId") String variantId,
            @Param("orderItemType") OrderItemType orderItemType,
            @Param("preOrderStatus") PreOrderStatus preOrderStatus,
            @Param("orderStatus") OrderStatus orderStatus
    );
}
