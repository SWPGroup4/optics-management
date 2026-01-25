package com.glassystem.optics.dto.response;

import java.math.BigDecimal;

import com.glassystem.optics.enums.ProductCategory;
import com.glassystem.optics.enums.ProductStatus;
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
public class ProductResponse {
	String id;
	String name;
	String brand;
	ProductCategory category;
	String frameType;
	String gender;
	String shape;
	String frameMaterial;
	String hingeType;
	String nosePadType;
	BigDecimal weightGram;
	ProductStatus status;
}
