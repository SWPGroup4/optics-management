package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
	String id;
	String name;
	String brand;
	String category;
	String frameType;
	String gender;
	String shape;
	String frameMaterial;
	String hingeType;
	String nosePadType;
	BigDecimal weightGram;
	BigDecimal minPrice;
	BigDecimal maxPrice;
	ProductStatus status;
    List<ProductImageResponse> imageUrl;
}
