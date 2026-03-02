package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO response cho mỗi item trong combo.
 *
 * - productId: ID sản phẩm (nếu combo chỉ yêu cầu product level)
 * - productName: Tên sản phẩm (tiện hiển thị FE)
 * - skuId: ID ProductVariant cụ thể (nếu combo yêu cầu đúng SKU)
 * - skuLabel: Thông tin mô tả variant (color + size) để FE hiển thị
 * - requiredQuantity: Số lượng tối thiểu phải mua
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboItemResponse {
	String id;
	String productId;
	String productName;
	String skuId;
	String skuLabel;
	Integer requiredQuantity;
}
