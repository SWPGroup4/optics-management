package com.glassystem.optics.controller;

import java.math.BigDecimal;
import java.time.Instant;

import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.ProductPageResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.ProductCategory;
import com.glassystem.optics.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
	ProductService productService;

	@PostMapping
	ApiResponse<ProductResponse> create(@RequestBody @Valid ProductUpsertRequest request) {
		return ApiResponse.<ProductResponse>builder().result(productService.create(request)).build();
	}

	@GetMapping("/{id}")
	ApiResponse<ProductResponse> getById(@PathVariable Integer id) {
		return ApiResponse.<ProductResponse>builder().result(productService.getById(id)).build();
	}

	@PutMapping("/{id}")
	ApiResponse<ProductResponse> update(@PathVariable Integer id, @RequestBody @Valid ProductUpsertRequest request) {
		return ApiResponse.<ProductResponse>builder().result(productService.update(id, request)).build();
	}

	@DeleteMapping("/{id}")
	ApiResponse<Void> delete(@PathVariable Integer id) {
		productService.delete(id);
		return ApiResponse.<Void>builder().build();
	}

	@GetMapping
	ApiResponse<ProductPageResponse> getProducts(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String brand,
			@RequestParam(required = false) ProductCategory category,
			@RequestParam(required = false) Boolean isPrescriptionRequired,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {
		Sort.Direction direction = Sort.Direction.fromString(sortDir);
		var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
		var resultPage = productService.getProducts(q, brand, category, isPrescriptionRequired, minPrice, maxPrice, createdFrom, createdTo, pageable);

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
