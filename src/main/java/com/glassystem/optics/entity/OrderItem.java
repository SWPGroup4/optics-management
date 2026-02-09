package com.glassystem.optics.entity;

import com.glassystem.optics.enums.OrderItemStatus;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    OrderItemType orderItemType;
    @Enumerated(EnumType.STRING)
    OrderItemStatus status;


    BigDecimal depositPrice;
    BigDecimal remainingPrice;





    @ManyToOne
    @JoinColumn(name = "order_id")
    Orders order;

//    @ManyToOne
//    @JoinColumn(name = "product_variant_id")
//    ProductVariant productVariant;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    Inventory inventory;

    @OneToOne
            @JoinColumn(name = "prescription_id")
    Prescription prescription;
}
