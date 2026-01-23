package com.glassystem.optics.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Integer id;

	@Column(nullable = false)
	String name;

	String brand;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	ProductCategory category;

	@Column(columnDefinition = "text")
	String description;

	@Column(name = "base_price", nullable = false, precision = 19, scale = 2)
	BigDecimal basePrice;

	@Column(name = "is_prescription_required", nullable = false)
	Boolean isPrescriptionRequired;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	Instant createdAt;
}
