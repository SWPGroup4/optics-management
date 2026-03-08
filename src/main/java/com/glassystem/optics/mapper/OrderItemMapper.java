package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.OrderItemResponse;
import com.glassystem.optics.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring", uses =  {PrescriptionMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderItem toOrderItem(OrderItemCreationRequest orderItemCreationRequest);

    @Mapping(target = "productVariantId", source = "inventory.productVariant.id")
    @Mapping(target = "orderItemId", source = "id")
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
}
