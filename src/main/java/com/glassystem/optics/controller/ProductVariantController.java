package com.glassystem.optics.controller;

import java.math.BigDecimal;
import java.util.List;

import com.glassystem.optics.dto.request.InventoryUpdateRequest;
import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductVariantPageResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-variants")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantController {
	ProductVariantService productVariantService;

	@PostMapping
	ApiResponse<ProductVariantResponse> create(@RequestBody @Valid ProductVariantRequest request) {
		return ApiResponse.<ProductVariantResponse>builder().result(productVariantService.create(request)).build();
	}

	@GetMapping("/{id}")
	ApiResponse<ProductVariantResponse> getById(@PathVariable String id) {
		return ApiResponse.<ProductVariantResponse>builder().result(productVariantService.getById(id)).build();
	}

	@PutMapping("/{id}")
	ApiResponse<ProductVariantResponse> update(@PathVariable String id, @RequestBody @Valid ProductVariantRequest request) {
		return ApiResponse.<ProductVariantResponse>builder().result(productVariantService.update(id, request)).build();
	}

    @PutMapping("/quantity")
    public ApiResponse<ProductVariantResponse> updateQuantity(@RequestBody @Valid InventoryUpdateRequest request) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(productVariantService.updateQuantity(request))
                .build();
    }

	@DeleteMapping("/{id}")
	ApiResponse<Void> delete(@PathVariable String id) {
		productVariantService.delete(id);
		return ApiResponse.<Void>builder().build();
	}

	@GetMapping
    ApiResponse<List<ProductVariantResponse>> getVariants() {
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(productVariantService.getVariants())
                .build();
    }
}
