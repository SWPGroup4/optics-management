package com.glassystem.optics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO cho POST /api/orders/price-check
 *
 * Tính toán giá cuối cùng sau khi áp dụng combo và phát hiện xung đột giá:
 * - Giảm giá vượt ngưỡng
 * - Giá bán thấp hơn mức cho phép
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceCheckRequest {

    @Valid
    @NotEmpty(message = "PRICE_CHECK_ITEMS_REQUIRED")
    List<PriceCheckItemRequest> items;

    String comboId;
}
