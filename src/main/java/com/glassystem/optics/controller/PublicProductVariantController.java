package com.glassystem.optics.controller;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductVariantCompareResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.service.ProductVariantService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/product-variants")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")

public class PublicProductVariantController {
	ProductVariantService productVariantService;

	/**
	 * API-04: Tìm Product Variant phù hợp theo thông số mặt kính
	 *
	 * Params:
	 * - lensWidthMm, bridgeWidthMm, templeLengthMm: bắt buộc
	 * - productId: optional (nếu muốn giới hạn search trong một product)
	 */
	@GetMapping("/nearest")
	ApiResponse<ProductVariantResponse> findNearestVariant(
			@RequestParam Integer lensWidthMm,
			@RequestParam Integer bridgeWidthMm,
			@RequestParam Integer templeLengthMm,
			@RequestParam(required = false) String productId) {
		return ApiResponse.<ProductVariantResponse>builder()
				.result(productVariantService.findNearestVariantBySize(lensWidthMm, bridgeWidthMm, templeLengthMm, productId))
				.build();
	}

	/**
	 * API-05: So sánh nhiều variants của cùng một product
	 *
	 * Note:
	 * - Dùng query param để giữ endpoint là GET (public theo SecurityConfig hiện tại).
	 * - Client gọi dạng: /public/product-variants/compare?productId=...&variantIds=a&variantIds=b
	 */
	@GetMapping("/compare")
	ApiResponse<ProductVariantCompareResponse> compareVariants(
			@RequestParam String productId,
			@RequestParam List<String> variantIds) {
		return ApiResponse.<ProductVariantCompareResponse>builder()
				.result(productVariantService.compareVariants(productId, variantIds))
				.build();
	}
}
