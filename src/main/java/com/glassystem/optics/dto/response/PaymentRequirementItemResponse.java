package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.OrderItemType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRequirementItemResponse {
    String orderItemId;
    OrderItemType orderItemType;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal itemTotal;
    double paymentPercentage;
    BigDecimal requiredPayment;
}
