package com.glassystem.optics.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "payment_calculation_result")
@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentCalculationResult {
    BigDecimal orderTotal;
    BigDecimal requiredPaymentTotal;
    List<PaymentCalculationService.ItemPaymentRequirement> itemRequirements;
}