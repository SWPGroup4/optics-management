package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO đại diện cho một item trong giỏ hàng khi validate combo.
 *
 * - skuId: ID của ProductVariant trong giỏ
 * - quantity: Số lượng khách đang mua
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemRequest {
	@NotBlank(message = "FIELD_MISSING")
	String skuId;

	@NotNull(message = "INVALID_QUANTITY")
	@Min(value = 1, message = "INVALID_QUANTITY")
	Integer quantity;
}
