package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.OrderCreationRequest;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.entity.Orders;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;



@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "depositAmount", ignore = true)
    @Mapping(target = "customer", ignore = true)
    Orders toOrder(OrderCreationRequest orderCreationRequest);


    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "orderStatus", source = "status")
    @Mapping(target = "comboId", source = "combo.id")
    @Mapping(target = "comboName", source = "combo.name")
    @Mapping(target = "comboDiscountAmount", source = "comboDiscountAmount")
    @Mapping(target = "comboSnapshot", source = "comboSnapshot")
    OrderResponse toOrderResponse(Orders order);
}
