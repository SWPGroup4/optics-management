package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "product", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	ProductVariant toProductVariant(ProductVariantRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "product", ignore = true)
	ProductVariant toEntity(ProductVariantRequest request);

	@Mapping(target = "productId", source = "product.id")
	ProductVariantResponse toResponse(ProductVariant variant);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "product", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	void updateEntity(@MappingTarget ProductVariant variant, ProductVariantRequest request);
}
