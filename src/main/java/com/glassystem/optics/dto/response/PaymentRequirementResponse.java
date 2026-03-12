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
    double depositPercentage;
    BigDecimal requiredAmount;
    BigDecimal orderTotal;
    BigDecimal requiredPaymentTotal;
    BigDecimal remainingPaymentTotal;
    List<PaymentRequirementItemResponse> itemRequirements;
    boolean allowCOD;
    String message;
}
