package com.glassystem.optics.controller;

import java.math.BigDecimal;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductPageResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.enums.ProductStatus;
import com.glassystem.optics.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
	ProductService productService;

	@PostMapping
	ApiResponse<ProductResponse> create(@RequestBody @Valid ProductCreateRequest request) {
		return ApiResponse.<ProductResponse>builder().result(productService.create(request)).build();
	}

	@GetMapping("/{id}")
	ApiResponse<ProductResponse> getById(@PathVariable String id) {
		return ApiResponse.<ProductResponse>builder().result(productService.getById(id)).build();
	}

	@PutMapping("/{id}")
	ApiResponse<ProductResponse> update(@PathVariable String id, @RequestBody @Valid ProductUpsertRequest request) {
		return ApiResponse.<ProductResponse>builder().result(productService.update(id, request)).build();
	}

	@DeleteMapping("/{id}")
	ApiResponse<Void> delete(@PathVariable String id) {
		productService.delete(id);
		return ApiResponse.<Void>builder().build();
	}

	@GetMapping
	ApiResponse<ProductPageResponse> getProducts(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String brand,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String frameType,
			@RequestParam(required = false) String gender,
			@RequestParam(required = false) String shape,
			@RequestParam(required = false) String frameMaterial,
			@RequestParam(required = false) String hingeType,
			@RequestParam(required = false) String nosePadType,
			@RequestParam(required = false) BigDecimal minWeightGram,
			@RequestParam(required = false) BigDecimal maxWeightGram,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {
		Sort.Direction direction = Sort.Direction.fromString(sortDir);
		var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
		var resultPage = productService.getProducts(
				q,
				brand,
				category,
				frameType,
				gender,
				shape,
				frameMaterial,
				hingeType,
				nosePadType,
				minWeightGram,
				maxWeightGram,
				status,
				pageable);

		ProductPageResponse response = ProductPageResponse.builder()
				.items(resultPage.getContent())
				.page(resultPage.getNumber())
				.size(resultPage.getSize())
				.totalElements(resultPage.getTotalElements())
				.totalPages(resultPage.getTotalPages())
				.build();

		return ApiResponse.<ProductPageResponse>builder().result(response).build();
	}
}
