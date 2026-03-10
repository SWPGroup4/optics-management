package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    @Mapping(source = "id", target = "refundId")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(target = "customerName", expression = "java(refund.getOrder().getCustomer().getFirstName() " +
            "+ \" \" + refund.getOrder().getCustomer().getLastName())")
    @Mapping(source = "bankAccountNumber", target = "bankAccount")
    RefundResponse toRefundResponse(Refund refund);


    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "refundAmount", source = "depositAmount")
    RefundResponse toRefundResponseFromOrder(Orders order);
}