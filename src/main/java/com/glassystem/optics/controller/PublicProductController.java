package com.glassystem.optics.controller;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductDetailResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicProductController {
	ProductService productService;

	/**
	 * API-06: Lấy chi tiết Product (Product + Variant + Image)
	 *
	 * Note:
	 * - Endpoint public phục vụ trang chi tiết sản phẩm (FE).
	 */
	@GetMapping("/{productId}/detail")
	ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable String productId) {
		return ApiResponse.<ProductDetailResponse>builder()
				.result(productService.getPublicProductDetail(productId))
				.build();
	}

	/**
	 * API-01: Gợi ý sản phẩm tương tự
	 *
	 * Rule:
	 * - Cùng category
	 * - Cùng gender
	 * - Khác product id hiện tại
	 */
	@GetMapping("/{productId}/similar")
	ApiResponse<List<ProductResponse>> getSimilarProducts(
			@PathVariable String productId,
			@RequestParam(defaultValue = "10") int limit) {
		return ApiResponse.<List<ProductResponse>>builder()
				.result(productService.getSimilarProducts(productId, limit))
				.build();
	}
}
