package com.glassystem.optics.controller.order;

import com.glassystem.optics.dto.request.PriceCheckRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.PriceCheckResponse;
import com.glassystem.optics.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý kiểm tra xung đột giá trước khi tạo đơn.
 *
 * POST /api/orders/price-check
 * - Tính toán giá cuối cùng sau khi áp dụng combo
 * - Phát hiện: giảm giá vượt ngưỡng, giá bán thấp hơn mức cho phép
 *
 * Người dùng: Sales
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Price & Conflict Validation", description = "Kiểm tra xung đột giá trước khi tạo đơn hàng")
@PreAuthorize("hasRole('SALE') or hasRole('ADMIN') or hasRole('OPERATION')")
public class OrderPriceCheckController {

    OrderService orderService;

    /**
     * Kiểm tra xung đột giá trước khi tạo đơn.
     *
     * Request body:
     * - items: danh sách sản phẩm (productVariantId + quantity)
     * - comboId: ID combo muốn áp dụng (optional)
     *
     * Response:
     * - originalTotal: Tổng giá gốc
     * - comboDiscount: Số tiền giảm từ combo
     * - finalTotal: Giá cuối cùng
     * - isValid: true nếu không có xung đột nghiêm trọng
     * - warnings: Danh sách cảnh báo (DISCOUNT_EXCEEDS_THRESHOLD, BELOW_MIN_PRICE, ...)
     */
    @PostMapping("/price-check")
    @Operation(summary = "Kiểm tra xung đột giá",
            description = "Tính giá cuối cùng sau combo và phát hiện giảm giá vượt ngưỡng hoặc giá thấp hơn mức cho phép")
    public ApiResponse<PriceCheckResponse> priceCheck(@RequestBody @Valid PriceCheckRequest request) {
        return ApiResponse.<PriceCheckResponse>builder()
                .result(orderService.priceCheck(request))
                .message("Kiểm tra giá thành công")
                .build();
    }
}
