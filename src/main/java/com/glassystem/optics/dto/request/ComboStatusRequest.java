package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.ComboStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO cho PATCH /api/combos/{comboId}/status
 *
 * Chỉ cho phép chuyển sang ACTIVE hoặc INACTIVE.
 * Các trạng thái khác (SCHEDULED, EXPIRED) do hệ thống tự quản lý.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboStatusRequest {
	@NotNull(message = "COMBO_STATUS_INVALID")
	ComboStatus status;
}
