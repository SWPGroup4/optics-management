package com.glassystem.optics.repository;

import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    List<OrderItem> findByProductVariantIdAndOrderItemTypeAndOrder_StatusOrderByOrder_CreatedAtAsc(
            String productVariantId,
            OrderItemType orderItemType,
            OrderStatus orderStatus
    );
}
