package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundResponse {

    String refundId;
    OrderResponse order;

    BigDecimal refundAmount;

    BigDecimal refundPercentage;

    BigDecimal deductionAmount;

    RefundStatus refundStatus;


}
