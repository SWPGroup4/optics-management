package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.OrderItemType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemCreationRequest {
    String productVariantId;
    OrderItemType orderItemType;
    Integer quantity;
    PrescriptionRequest prescription;
}
