package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.OrderItemResponse;
import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Mapper(componentModel = "spring", uses =  {PrescriptionMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderItem toOrderItem(OrderItemCreationRequest orderItemCreationRequest);

    @Mapping(source = "productVariant.id", target = "productVariantId")
    @Mapping(target = "orderItemId", source = "id")
    @Mapping(target = "productName", expression = "java(getProductName(orderItem))")
    @Mapping(target = "variantName", expression = "java(buildVariantName(orderItem.getProductVariant()))")
    @Mapping(target = "itemName", expression = "java(buildItemName(orderItem))")
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
        if (orderItem == null || orderItem.getProductVariant() == null || orderItem.getProductVariant().getProduct() == null) {
            return null;
        }
        return orderItem.getProductVariant().getProduct().getName();
    }

    default String buildVariantName(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        return Stream.of(variant.getColorName(), variant.getSizeLabel(), variant.getFrameFinish())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" - "));
    }

    default String buildItemName(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        String productName = getProductName(orderItem);
        String variantName = buildVariantName(orderItem.getProductVariant());

        if (productName == null && variantName == null) {
            return orderItem.getProductVariant() != null ? orderItem.getProductVariant().getId() : null;
        }
        if (variantName == null || variantName.isBlank()) {
            return productName;
        }
        if (productName == null || productName.isBlank()) {
            return variantName;
        }
        return productName + " - " + variantName;
    }
}
