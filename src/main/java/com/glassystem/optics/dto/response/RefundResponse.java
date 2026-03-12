package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundResponse {

    String refundId;

    String orderId;

    String variantId;

    String customerName;

    String bankName;

    String bankAccount;

    BigDecimal orderTotalAmount;

    BigDecimal refundAmount;

    RefundStatus refundStatus;


}