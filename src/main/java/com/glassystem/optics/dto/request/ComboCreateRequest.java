package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tạo mới Combo.
 *
 * Bao gồm thông tin cơ bản của combo và danh sách các item.
 * Validation cơ bản qua annotation, validation nghiệp vụ nâng cao xử lý trong Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboCreateRequest {
	@NotBlank(message = "COMBO_NAME_REQUIRED")
	String name;

	String description;

	@NotNull(message = "COMBO_DISCOUNT_TYPE_REQUIRED")
	DiscountType discountType;

	@NotNull(message = "COMBO_DISCOUNT_VALUE_INVALID")
	@Positive(message = "COMBO_DISCOUNT_VALUE_INVALID")
	BigDecimal discountValue;

	@NotNull(message = "COMBO_TIME_REQUIRED")
	LocalDateTime startTime;

	@NotNull(message = "COMBO_TIME_REQUIRED")
	LocalDateTime endTime;

	Boolean isManuallyDisabled;

	@NotEmpty(message = "COMBO_ITEMS_REQUIRED")
	@Valid
	List<ComboItemRequest> comboItems;
}
