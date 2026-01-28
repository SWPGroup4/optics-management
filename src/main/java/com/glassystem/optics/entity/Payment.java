package com.glassystem.optics.entity;


import com.glassystem.optics.enums.PaymentMethod;
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
    String paymentPurpose;
    BigDecimal amount;
    String status;
    LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Orders order;

}
