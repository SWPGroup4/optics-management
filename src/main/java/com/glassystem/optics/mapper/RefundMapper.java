package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.RefundBankAccountResponse;
import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Refund;
import com.glassystem.optics.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    @Mapping(source = "id", target = "refundId")
    @Mapping(source = "order.id", target = "orderId")
//    @Mapping(source = "variantId", target = "variantId")
//    @Mapping(source = "orderTotalAmount", target = "orderTotalAmount")
//    @Mapping(source = "refundAmount", target = "refundAmount")
//    @Mapping(target = "refundPercentage", expression = "refundPercentage")
    @Mapping(source = "status", target = "refundStatus")
    @Mapping(target = "customerName", expression = "java(resolveDisplayCustomerName(refund))")
    @Mapping(source = "bankAccountNumber", target = "bankAccount")
    RefundResponse toRefundResponse(Refund refund);


//    @Mapping(target = "orderId", source = "id")
//    @Mapping(target = "orderTotalAmount", source = "totalAmount")
//    @Mapping(target = "refundAmount", source = "depositAmount")
//    @Mapping(target = "refundPercentage", expression = "java(java.math.BigDecimal.ZERO)")
//    RefundResponse toRefundResponseFromOrder(Orders order);

    RefundBankAccountResponse toRefundBankAccountResponse (Refund refund);

    default String resolveDisplayCustomerName(Refund refund) {
        if (refund == null) {
            return null;
        }
        if (refund.getAccountHolderName() != null && !refund.getAccountHolderName().isBlank()) {
            return refund.getAccountHolderName().trim();
        }
        return buildCustomerName(refund.getOrder() != null ? refund.getOrder().getCustomer() : null);
    }

    default String buildCustomerName(User customer) {
        if (customer == null) {
            return null;
        }

        String firstName = customer.getFirstName() == null ? "" : customer.getFirstName().trim();
        String lastName = customer.getLastName() == null ? "" : customer.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }
        if (customer.getUsername() != null && !customer.getUsername().isBlank()) {
            return customer.getUsername();
        }
        return customer.getEmail();
    }
}
