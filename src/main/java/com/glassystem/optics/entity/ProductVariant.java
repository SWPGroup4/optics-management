package com.glassystem.optics.entity;

import com.glassystem.optics.enums.OrderItemType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Where;

import com.glassystem.optics.enums.ProductVariantStatus;

import java.math.BigDecimal;


@Entity
@Table(name = "product_variant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Where(clause = "is_deleted = false")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "color_name")
    String colorName;

    @Column(name = "frame_finish")
    String frameFinish;

    @Column(name = "lens_width_mm")
    Integer lensWidthMm;

    @Column(name = "bridge_width_mm")
    Integer bridgeWidthMm;

    @Column(name = "temple_length_mm")
    Integer templeLengthMm;

    @Column(name = "size_label")
    String sizeLabel;

    @Column(precision = 12, scale = 2)
    BigDecimal price;
    @Column(name = "quantity")
    Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProductVariantStatus status;


    @Enumerated(EnumType.STRING)
    @Column(name = "order_item_type", nullable = false)
    OrderItemType orderItemType;


    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

}
