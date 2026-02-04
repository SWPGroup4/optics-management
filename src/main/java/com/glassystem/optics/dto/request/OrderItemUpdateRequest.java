package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemUpdateRequest {
    @NotNull(message = "QUANTITY_INVALID")
    String orderItemId;

    @NotNull(message = "QUANTITY_INVALID")
    @Min(value = 1, message = "INVALID_QUANTITY")
    Integer quantity;

    PrescriptionRequest prescription;
}
