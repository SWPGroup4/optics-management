package com.glassystem.optics.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRequirementResponse {
    double depositPercentage;    // 0, 0.5 hoặc 1.0
    BigDecimal requiredAmount;   // Số tiền cụ thể cần cọc

    BigDecimal orderTotal;
    BigDecimal requiredPaymentTotal;
     List<PaymentRequirementItemResponse> itemRequirements;

    boolean allowCOD;            // Có cho phép chọn COD không
    String message;              // Thông báo giải thích quy tắc
}
