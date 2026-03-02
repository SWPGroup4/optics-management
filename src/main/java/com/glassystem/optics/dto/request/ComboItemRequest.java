package com.glassystem.optics.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO cho mỗi item trong combo khi tạo/cập nhật.
 *
 * - productId: ID sản phẩm (dùng khi combo chấp nhận bất kỳ variant nào của product)
 * - skuId: ID của ProductVariant cụ thể (dùng khi combo yêu cầu đúng variant)
 * - requiredQuantity: Số lượng tối thiểu khách phải mua
 *
 * Lưu ý: Phải truyền ít nhất 1 trong 2 (productId hoặc skuId).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboItemRequest {
	String productId;
	String skuId;
	Integer requiredQuantity;
}
