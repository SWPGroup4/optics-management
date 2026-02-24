package com.glassystem.optics.configuration;

import com.glassystem.optics.service.ComboService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background Job — Tự động đồng bộ trạng thái Combo.
 *
 * Chạy mỗi phút (cron: 0 * * * * *) để cập nhật trạng thái combo:
 * - now < startTime → SCHEDULED
 * - now ∈ [startTime, endTime] → ACTIVE
 * - now > endTime → EXPIRED
 *
 * Lưu ý: KHÔNG override combo bị disable thủ công (isManuallyDisabled = true).
 * Những combo đó chỉ được bật lại bởi Manager qua API PATCH /api/combos/{comboId}/status.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ComboStatusScheduler {
	ComboService comboService;

	/**
	 * Cron job chạy mỗi phút.
	 * Gọi comboService.syncComboStatuses() để tính lại trạng thái tất cả combo
	 * chưa bị disable thủ công.
	 */
	@Scheduled(cron = "0 * * * * *")
	public void syncComboStatuses() {
		log.debug("Bắt đầu sync trạng thái combo...");
		comboService.syncComboStatuses();
	}
}
