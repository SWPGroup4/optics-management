package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryUpdateRequest {
    @NotNull(message = "VARIANT_ID_REQUIRED")
    String productVariantId;

    @NotNull(message = "QUANTITY_REQUIRED")
    Integer changeAmount; // Số dương để nhập hàng, số âm để xuất kho
}