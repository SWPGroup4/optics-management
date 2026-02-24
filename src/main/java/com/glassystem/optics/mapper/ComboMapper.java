package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.ComboItemResponse;
import com.glassystem.optics.dto.response.ComboResponse;
import com.glassystem.optics.entity.Combo;
import com.glassystem.optics.entity.ComboItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper cho Combo và ComboItem.
 *
 * Chuyển đổi entity sang DTO response.
 * Các trường phức tạp (productName, skuLabel) được tính bằng default method.
 */
@Mapper(componentModel = "spring")
public interface ComboMapper {

	@Mapping(target = "comboItems", source = "comboItems")
	ComboResponse toComboResponse(Combo combo);

	@Mapping(target = "productId", expression = "java(item.getProduct() != null ? item.getProduct().getId() : null)")
	@Mapping(target = "productName", expression = "java(item.getProduct() != null ? item.getProduct().getName() : null)")
	@Mapping(target = "skuId", expression = "java(item.getProductVariant() != null ? item.getProductVariant().getId() : null)")
	@Mapping(target = "skuLabel", expression = "java(buildSkuLabel(item))")
	ComboItemResponse toComboItemResponse(ComboItem item);

	/**
	 * Tạo label mô tả variant để FE hiển thị.
	 * Format: "colorName - sizeLabel" (nếu có)
	 */
	default String buildSkuLabel(ComboItem item) {
		if (item.getProductVariant() == null) {
			return null;
		}
		String color = item.getProductVariant().getColorName();
		String size = item.getProductVariant().getSizeLabel();
		if (color != null && size != null) {
			return color + " - " + size;
		}
		if (color != null) return color;
		if (size != null) return size;
		return item.getProductVariant().getId();
	}
}
