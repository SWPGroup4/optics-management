package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * DTO response cho POST /api/combos/validate
 *
 * - isValid: true nếu giỏ hàng thỏa mãn tất cả rule của combo
 * - discountAmount: Số tiền được giảm (chỉ có khi isValid = true)
 * - reason: Lý do không hợp lệ (OUT_OF_STOCK, NOT_MATCH_RULE, EXPIRED, NOT_ACTIVE...)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboValidateResponse {
	Boolean isValid;
	BigDecimal discountAmount;
	String reason;
}
