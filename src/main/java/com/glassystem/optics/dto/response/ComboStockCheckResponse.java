package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO response cho POST /api/combos/check-stock
 *
 * - isAvailable: true nếu tất cả item trong combo đều đủ tồn kho
 * - failedItems: danh sách item bị thiếu hàng (chỉ có khi isAvailable = false)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboStockCheckResponse {
	Boolean isAvailable;
	List<FailedStockItem> failedItems;

	/**
	 * Thông tin chi tiết item bị thiếu hàng.
	 *
	 * - skuId: ID ProductVariant
	 * - requiredQuantity: Số lượng combo yêu cầu
	 * - availableQuantity: Số lượng thực tế có thể bán (onHand - reserved)
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class FailedStockItem {
		String skuId;
		Integer requiredQuantity;
		Integer availableQuantity;
	}
}
