package com.glassystem.optics.entity;


import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {
    @Id
            @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    TransactionType type;
    BigDecimal amount;
    String gatewayReference;
    LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    Payment payment;

}
