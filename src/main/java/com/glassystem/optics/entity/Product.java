package com.glassystem.optics.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.glassystem.optics.enums.ProductCategory;
import com.glassystem.optics.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Where(clause = "is_deleted = false")
public class Product {

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String name;

    String brand;

    @Enumerated(EnumType.STRING)
    ProductCategory category;

    @Column(name = "frame_type")
    String frameType;

    String gender;

    String shape;

    @Column(name = "frame_material")
    String frameMaterial;

    @Column(name = "hinge_type")
    String hingeType;

    @Column(name = "nose_pad_type")
    String nosePadType;

    @Column(name = "weight_gram", precision = 6, scale = 2)
    BigDecimal weightGram;

    @Column(name = "model_url")
    String modelUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    List<ProductImage> imageUrl = new ArrayList<>();

}
