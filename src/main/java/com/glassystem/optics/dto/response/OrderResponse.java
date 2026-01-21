package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.OrderType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    String customerId;
    String orderId;
    OrderType orderType;
    OrderStatus orderStatus;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    List<OrderItemResponse> items;
}
