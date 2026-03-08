package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.ProductVariantStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantResponse {
	String id;
	String productId;
	String colorName;
	String frameFinish;
	Integer lensWidthMm;
	Integer bridgeWidthMm;
	Integer templeLengthMm;
	String sizeLabel;
	BigDecimal price;
	ProductVariantStatus status;

    OrderItemType orderItemType;

}
