package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentPurpose;
import com.glassystem.optics.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
     String id;
     PaymentMethod paymentMethod;
     PaymentPurpose paymentPurpose;
     BigDecimal amount;
     BigDecimal percentage;
     PaymentStatus status;
     LocalDateTime paymentDate;
     String description;
     String transactionReference;
}
