package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductImageResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductImage;
import com.glassystem.optics.enums.ProductVariantStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.Comparator;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Product toProduct(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Product toProduct(ProductUpsertRequest request);

    @Mapping(target = "minPrice", expression = "java(calculateMinPrice(product))")
    @Mapping(target = "maxPrice", expression = "java(calculateMaxPrice(product))")
    ProductResponse toProductResponse(Product product);


    default BigDecimal calculateMinPrice(Product product) {
        if (product == null || product.getVariants() == null) {
            return null;
        }

        return product.getVariants().stream()
                .filter(v -> v != null && v.getStatus() == ProductVariantStatus.ACTIVE && v.getPrice() != null)
                .map(v -> v.getPrice())
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    default BigDecimal calculateMaxPrice(Product product) {
        if (product == null || product.getVariants() == null) {
            return null;
        }

        return product.getVariants().stream()
                .filter(v -> v != null && v.getStatus() == ProductVariantStatus.ACTIVE && v.getPrice() != null)
                .map(v -> v.getPrice())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductUpsertRequest request);
}
