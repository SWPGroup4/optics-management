package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    String customerId;
    String orderId;
    String orderName;
    String deliveryAddress;
    String phoneNumber;
    OrderStatus orderStatus;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    BigDecimal remainingAmount;
    BigDecimal paidAmount;
    List<OrderItemResponse> items;
    List<PaymentResponse> payments;

    String comboId;
    String comboName;
    BigDecimal comboDiscountAmount;
    String comboSnapshot;

    BigDecimal refundedAmount;
    BigDecimal finalTotalAfterRefund;

    BankInfoResponse bankInfo;

}
