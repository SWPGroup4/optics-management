package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryQuantityUpdateResponse {
    ProductVariantResponse productVariant;
    Integer updatedOrderCount;
    List<OrderResponse> updatedOrders;
}
