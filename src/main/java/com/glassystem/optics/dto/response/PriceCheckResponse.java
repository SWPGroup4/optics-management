package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO response cho POST /api/orders/price-check
 *
 * Trả về giá cuối cùng sau khi áp dụng combo và cảnh báo xung đột giá:
 * - originalTotal: Tổng giá gốc
 * - comboDiscount: Số tiền giảm từ combo
 * - finalTotal: Giá cuối cùng sau giảm
 * - warnings: Danh sách cảnh báo xung đột giá (nếu có)
 * - isValid: true nếu không có xung đột nghiêm trọng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceCheckResponse {
    BigDecimal originalTotal;
    BigDecimal comboDiscount;
    BigDecimal finalTotal;
    Boolean isValid;
    List<PriceCheckItemDetail> itemDetails;
    List<PriceConflictWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceCheckItemDetail {
        String productVariantId;
        String productName;
        String skuLabel;
        Integer quantity;
        BigDecimal unitPrice;
        BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceConflictWarning {
        String type;
        String message;
        BigDecimal threshold;
        BigDecimal actualValue;
    }
}
