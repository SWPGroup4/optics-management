package com.glassystem.optics.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "product_image")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {
    @Id
            @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String imageUrl;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;
}
