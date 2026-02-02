package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.OrderItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemCreationRequest {
    String productVariantId;
    @NotNull(message = "INVALID_ORDER_ITEM_TYPE")
    OrderItemType orderItemType;
    @NotNull(message = "QUANTITY_INVALID")
    @Min(value = 1, message = "INVALID_QUANTITY")
    Integer quantity;
    PrescriptionRequest prescription;
}
