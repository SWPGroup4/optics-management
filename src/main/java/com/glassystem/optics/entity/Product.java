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

@Entity
@Table(name = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	ProductStatus status;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    List<ProductImage> imageUrl;
}
// product status acctive