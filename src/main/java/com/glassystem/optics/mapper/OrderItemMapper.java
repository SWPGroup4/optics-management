package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.OrderCreationRequest;
import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.OrderItemResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.Orders;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring", uses =  {PrescriptionMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    OrderItem toOrderItem(OrderItemCreationRequest orderItemCreationRequest);

    @Mapping(target = "productVariantId", source = "inventory.productVariant.id")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);


    List<OrderItem> toOrderItemList(List<OrderItemCreationRequest> orderItemCreationRequests);
    List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems);
}
