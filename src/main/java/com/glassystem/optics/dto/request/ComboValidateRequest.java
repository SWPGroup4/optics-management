package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO cho POST /api/combos/validate
 *
 * Dùng để kiểm tra combo có áp dụng được cho giỏ hàng hiện tại không.
 * - comboId: ID combo cần validate
 * - cartItems: danh sách item trong giỏ hàng (skuId + quantity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboValidateRequest {
	@NotBlank(message = "COMBO_NOT_FOUND")
	String comboId;

	@NotEmpty(message = "COMBO_ITEMS_REQUIRED")
	List<CartItemRequest> cartItems;
}
