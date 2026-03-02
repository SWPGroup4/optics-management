package com.glassystem.optics.entity;

import com.glassystem.optics.enums.ComboStatus;
import com.glassystem.optics.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một Combo khuyến mãi.
 *
 * Combo bao gồm nhiều ComboItem (sản phẩm/variant cần mua).
 * Khi khách hàng mua đủ các item theo yêu cầu, sẽ được giảm giá
 * theo discountType (PERCENT hoặc FIXED_AMOUNT) với giá trị discountValue.
 *
 * Trạng thái combo được quản lý tự động bởi background job
 * và có thể bị override thủ công bởi Manager (isManuallyDisabled).
 */
@Entity
@Table(name = "combo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Combo {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	String id;

	@Column(nullable = false, columnDefinition = "TEXT")

	String name;

	@Column(columnDefinition = "TEXT")
	String description;

	/** Loại giảm giá: PERCENT hoặc FIXED_AMOUNT */
	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false)
	DiscountType discountType;

	/** Giá trị giảm giá (ví dụ: 10 cho 10%, hoặc 50000 cho giảm 50k) */
	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	BigDecimal discountValue;

	/** Thời điểm combo bắt đầu có hiệu lực */
	@Column(name = "start_time", nullable = false)
	LocalDateTime startTime;

	/** Thời điểm combo hết hiệu lực */
	@Column(name = "end_time", nullable = false)
	LocalDateTime endTime;

	/** Trạng thái hiện tại: SCHEDULED, ACTIVE, INACTIVE, EXPIRED */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	ComboStatus status;

	/**
	 * Cờ tắt thủ công bởi Manager.
	 * Nếu true, background job sẽ KHÔNG tự động bật lại combo này.
	 */
	@Column(name = "is_manually_disabled", nullable = false)
	@Builder.Default
	Boolean isManuallyDisabled = false;

	@Column(name = "created_at")
	LocalDateTime createdAt;

	@Column(name = "updated_at")
	LocalDateTime updatedAt;

	/** Danh sách các item trong combo */
	@OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	List<ComboItem> comboItems = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
