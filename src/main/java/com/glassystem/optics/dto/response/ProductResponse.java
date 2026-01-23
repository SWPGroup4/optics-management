package com.glassystem.optics.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.glassystem.optics.entity.ProductCategory;
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
	Integer id;
	String name;
	String brand;
	ProductCategory category;
	String description;
	BigDecimal basePrice;
	Boolean isPrescriptionRequired;
	Instant createdAt;
}
