package com.glassystem.optics.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "inventory")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    Integer quantity;
    Integer reservedQuantity;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    List<OrderItem> items;

    @OneToOne
    @JoinColumn(name = "product_variant_id")
    ProductVariant productVariant;
}
