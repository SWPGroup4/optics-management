package com.glassystem.optics.entity;

import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Enumerated(EnumType.STRING)
    OrderStatus status;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    LocalDate createdAt;

    String deliveryAddress;
    String phoneNumber;
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;


    @ManyToOne
    @JoinColumn(name = "customer_id")
    User customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
            @Builder.Default
    List<OrderItem> items = new ArrayList<>();

}
