package com.glassystem.optics.enums;

/**
 * Trạng thái của Combo khuyến mãi.
 *
 * - SCHEDULED: Combo đã tạo nhưng chưa đến thời gian bắt đầu
 * - ACTIVE: Combo đang trong thời gian hiệu lực và được bật
 * - INACTIVE: Combo bị tắt thủ công bởi Manager
 * - EXPIRED: Combo đã hết hạn (quá endTime)
 */
public enum ComboStatus {
	ACTIVE,
	INACTIVE,
	SCHEDULED,
	EXPIRED,
}
