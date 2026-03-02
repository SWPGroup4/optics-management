package com.glassystem.optics.entity;


import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusHistory {
    @Id
            @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String orderId;

    @Enumerated(EnumType.STRING)
    OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    OrderStatus newStatus;

    LocalDateTime changedAt;

}
