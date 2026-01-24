package com.glassystem.optics.dto.request;

import java.math.BigDecimal;

import com.glassystem.optics.enums.ProductStatus;
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
public class ProductCreateRequest {
	@NotBlank(message = "PRODUCT_NAME_REQUIRED")
	String name;

	String brand;
	String category;
	String frameType;
	String gender;
	String shape;
	String frameMaterial;
	String hingeType;
	String nosePadType;

	@PositiveOrZero(message = "PRODUCT_WEIGHT_INVALID")
	BigDecimal weightGram;

	@NotNull(message = "PRODUCT_STATUS_REQUIRED")
	ProductStatus status;
}
