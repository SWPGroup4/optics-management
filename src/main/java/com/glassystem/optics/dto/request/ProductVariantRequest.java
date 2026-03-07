package com.glassystem.optics.dto.request;

import java.math.BigDecimal;

import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.ProductVariantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class ProductVariantRequest {
	@NotBlank(message = "PRODUCT_VARIANT_PRODUCT_REQUIRED")
	String productId;

	String colorName;
	String frameFinish;
	@Positive(message = "PRODUCT_VARIANT_LENS_WIDTH_INVALID")
	Integer lensWidthMm;

	@Positive(message = "PRODUCT_VARIANT_BRIDGE_WIDTH_INVALID")
	Integer bridgeWidthMm;

	@Positive(message = "PRODUCT_VARIANT_TEMPLE_LENGTH_INVALID")
	Integer templeLengthMm;
	String sizeLabel;

	@NotNull(message = "PRODUCT_VARIANT_PRICE_REQUIRED")
	@PositiveOrZero(message = "PRODUCT_VARIANT_PRICE_INVALID")
	BigDecimal price;

    Integer quantity;

	@NotNull(message = "PRODUCT_VARIANT_STATUS_REQUIRED")
	ProductVariantStatus status;

    @NotNull(message = "PRODUCT_PRESCRIPTION_REQUIRED")
    OrderItemType orderItemType;

}
