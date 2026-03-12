package com.glassystem.optics.entity;

import com.glassystem.optics.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refund_requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Orders order;

    String customerId;

    String variantId;

    BigDecimal orderTotalAmount;

    BigDecimal refundAmount;

    String bankName;

    String bankAccountNumber;

    String accountHolderName;

    @Enumerated(EnumType.STRING)
    RefundStatus status;

    LocalDateTime createdAt;

    LocalDateTime completedAt;

    String processedBy;
}
