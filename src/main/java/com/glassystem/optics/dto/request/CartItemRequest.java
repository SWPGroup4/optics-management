package com.glassystem.optics.dto.request;

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
	String skuId;
	Integer quantity;
}
