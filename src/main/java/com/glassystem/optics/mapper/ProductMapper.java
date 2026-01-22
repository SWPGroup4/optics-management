package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	Product toProduct(ProductUpsertRequest request);

	ProductResponse toProductResponse(Product product);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	void updateProduct(@MappingTarget Product product, ProductUpsertRequest request);
}
