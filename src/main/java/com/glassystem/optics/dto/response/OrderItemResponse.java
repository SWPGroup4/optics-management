package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.OrderItemStatus;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
    String productVariantId;
    OrderItemType orderItemType;
    Integer quantity;
    BigDecimal unitPrice;
    OrderItemStatus status;
    PrescriptionResponse prescription;

}
