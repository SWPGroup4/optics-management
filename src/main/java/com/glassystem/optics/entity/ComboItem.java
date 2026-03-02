package com.glassystem.optics.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity đại diện cho một item trong Combo.
 *
 * Mỗi ComboItem liên kết tới một ProductVariant (SKU) cụ thể
 * và yêu cầu khách hàng phải mua đủ requiredQuantity để combo có hiệu lực.
 */
@Entity
@Table(name = "combo_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	String id;

	/** Combo chứa item này */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "combo_id", nullable = false)
	Combo combo;

	/** Product (optional - dùng khi combo chỉ yêu cầu bất kỳ variant nào của product) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	Product product;

	/** ProductVariant / SKU cụ thể (optional - dùng khi combo yêu cầu đúng variant) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_variant_id")
	ProductVariant productVariant;

	/** Số lượng tối thiểu khách phải mua cho item này */
	@Column(name = "required_quantity", nullable = false)
	Integer requiredQuantity;
}
