package com.glassystem.optics.dto.response;

import java.math.BigDecimal;

import com.glassystem.optics.enums.ProductVariantStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

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
}
