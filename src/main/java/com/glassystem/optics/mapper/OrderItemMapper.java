package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.OrderItemResponse;
import com.glassystem.optics.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PrescriptionMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderItem toOrderItem(OrderItemCreationRequest orderItemCreationRequest);

    @Mapping(source = "productVariant.id", target = "productVariantId")
    @Mapping(target = "productId", expression = "java(getProductId(orderItem))")
    @Mapping(target = "orderItemId", source = "id")
    @Mapping(target = "productName", expression = "java(getProductName(orderItem))")
    @Mapping(target = "productImage", expression = "java(getProductImage(orderItem))")
    @Mapping(target = "variantName", expression = "java(buildVariantName(orderItem))")
    @Mapping(target = "itemName", expression = "java(getProductName(orderItem))")
    @Mapping(target = "lensPriceTotal", expression = "java(calculateLensPriceTotal(orderItem))")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    List<OrderItem> toOrderItemList(List<OrderItemCreationRequest> orderItemCreationRequests);
    List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems);

    default java.math.BigDecimal calculateLensPriceTotal(OrderItem orderItem) {
        java.math.BigDecimal feePerUnit = orderItem.getLensPrice() == null
                ? java.math.BigDecimal.ZERO
                : orderItem.getLensPrice();
        return feePerUnit.multiply(java.math.BigDecimal.valueOf(orderItem.getQuantity()));
    }

    default String getProductName(OrderItem orderItem) {
        if (orderItem == null
                || orderItem.getProductVariant() == null
                || orderItem.getProductVariant().getProduct() == null) {
            return null;
        }
        return orderItem.getProductVariant().getProduct().getName();
    }

    default String getProductId(OrderItem orderItem) {
        if (orderItem == null
                || orderItem.getProductVariant() == null
                || orderItem.getProductVariant().getProduct() == null) {
            return null;
        }
        return orderItem.getProductVariant().getProduct().getId();
    }

    default String getProductImage(OrderItem orderItem) {
        if (orderItem == null
                || orderItem.getProductVariant() == null
                || orderItem.getProductVariant().getProduct() == null
                || orderItem.getProductVariant().getProduct().getImageUrl() == null) {
            return null;
        }

        return orderItem.getProductVariant().getProduct().getImageUrl().stream()
                .filter(java.util.Objects::nonNull)
                .map(com.glassystem.optics.entity.ProductImage::getImageUrl)
                .filter(java.util.Objects::nonNull)
                .filter(url -> !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    default String buildVariantName(OrderItem orderItem) {
        if (orderItem == null || orderItem.getProductVariant() == null) {
            return null;
        }

        String color = orderItem.getProductVariant().getColorName();
        String size = orderItem.getProductVariant().getSizeLabel();
        String finish = orderItem.getProductVariant().getFrameFinish();

        java.util.List<String> parts = new java.util.ArrayList<>();
        if (color != null && !color.isBlank()) parts.add(color);
        if (size != null && !size.isBlank()) parts.add(size);
        if (finish != null && !finish.isBlank()) parts.add(finish);

        return parts.isEmpty() ? null : String.join(" - ", parts);
    }
}
