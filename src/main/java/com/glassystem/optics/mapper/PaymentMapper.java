package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.RoleRequest;
import com.glassystem.optics.dto.response.PaymentResponse;
import com.glassystem.optics.dto.response.RoleResponse;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);
}
