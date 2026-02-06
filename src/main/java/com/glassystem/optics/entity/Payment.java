package com.glassystem.optics.entity;


import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentPurpose;
import com.glassystem.optics.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
            @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    PaymentMethod paymentMethod;
    PaymentPurpose paymentPurpose;
    BigDecimal amount;
    PaymentStatus status;
    LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Orders order;

}
