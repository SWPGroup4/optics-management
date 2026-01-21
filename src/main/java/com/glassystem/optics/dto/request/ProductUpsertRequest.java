package com.glassystem.optics.dto.request;

import java.math.BigDecimal;

import com.glassystem.optics.entity.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductUpsertRequest {
	@NotBlank(message = "PRODUCT_NAME_REQUIRED")
	String name;

	String brand;

	@NotNull(message = "PRODUCT_CATEGORY_REQUIRED")
	ProductCategory category;

	String description;

	@NotNull(message = "PRODUCT_BASE_PRICE_REQUIRED")
	@PositiveOrZero(message = "PRODUCT_BASE_PRICE_INVALID")
	BigDecimal basePrice;

	@NotNull(message = "PRODUCT_PRESCRIPTION_REQUIRED")
	Boolean isPrescriptionRequired;
}
