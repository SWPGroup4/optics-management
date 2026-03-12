package com.glassystem.optics.controller.product;

import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-variants")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantController {
	ProductVariantService productVariantService;

	@PreAuthorize("permitAll()")

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


	@DeleteMapping("/{id}")
	ApiResponse<Void> delete(@PathVariable String id) {
		productVariantService.delete(id);
		return ApiResponse.<Void>builder().build();
	}


}
