package com.glassystem.optics.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemCreationRequest {
    String productVariantId;
    Integer quantity;
    PrescriptionRequest prescription;
}
