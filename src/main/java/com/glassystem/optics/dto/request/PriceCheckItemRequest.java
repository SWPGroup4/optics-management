package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO cho mỗi item trong price-check request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceCheckItemRequest {

    @NotBlank(message = "FIELD_MISSING")
    String productVariantId;

    @NotNull(message = "INVALID_QUANTITY")
    @Min(value = 1, message = "INVALID_QUANTITY")
    Integer quantity;
}
