package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variants", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
	Product toProduct(ProductCreateRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variants", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
	Product toProduct(ProductUpsertRequest request);

    @Mapping(target = "imageUrl", expression = "java(mapImages(product))")
	ProductResponse toProductResponse(Product product);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variants", ignore = true)
	void updateProduct(@MappingTarget Product product, ProductUpsertRequest request);

    default List<String> mapImages (Product product) {
        if(product.getImageUrl()==null) return null;
        return product.getImageUrl().stream().map(ProductImage::getImageUrl).toList();
    }
}
