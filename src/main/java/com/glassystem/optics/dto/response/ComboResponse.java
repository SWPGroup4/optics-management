package com.glassystem.optics.dto.response;

import com.glassystem.optics.enums.ComboStatus;
import com.glassystem.optics.enums.DiscountType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO response cho Combo (dùng cho cả list và detail).
 *
 * Bao gồm đầy đủ thông tin combo + danh sách items.
 * Trạng thái (status) được tính toán realtime trong service
 * dựa trên thời gian hiện tại và cờ isManuallyDisabled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboResponse {
	String id;
	String name;
	String description;
	DiscountType discountType;
	BigDecimal discountValue;
	LocalDateTime startTime;
	LocalDateTime endTime;
	ComboStatus status;
	Boolean isManuallyDisabled;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
	List<ComboItemResponse> comboItems;
}
