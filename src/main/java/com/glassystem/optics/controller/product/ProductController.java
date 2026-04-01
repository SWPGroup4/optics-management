package com.glassystem.optics.controller.product;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.*;


import com.glassystem.optics.enums.ProductStatus;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.service.ProductService;
import com.glassystem.optics.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)



public class ProductController {
	ProductService productService;
    ProductVariantService productVariantService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("permitAll()")
	ApiResponse<ProductResponse> create(
			@RequestPart("product") @Valid ProductCreateRequest request,
			@RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
		return ApiResponse.<ProductResponse>builder()
				.result(productService.create(request, files))
				.message("Product created successfully")
				.build();
	}

	@GetMapping("/{id}")
	ApiResponse<ProductResponse> getById(@PathVariable String id) {
		return ApiResponse.<ProductResponse>builder().result(productService.getById(id)).build();
	}

	@PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
	ApiResponse<ProductResponse> update(@PathVariable String id, @RequestBody @Valid ProductUpsertRequest request) {
		return ApiResponse.<ProductResponse>builder().result(productService.update(id, request)).build();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
	ApiResponse<Void> delete(@PathVariable String id) {
		productService.delete(id);
		return ApiResponse.<Void>builder()
				.message("Product deleted successfully")
				.build();
	}

    @PostMapping(value = "/{productId}/images", consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<ProductImageResponse>> uploadImages(
            @PathVariable String productId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(productService.uploadProductImages(productId, files))
                .message("Uploaded successfully")
                .build();
    }

    @DeleteMapping("/images/{imageId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteImage(@PathVariable String imageId) {
        productService.deleteProductImage(imageId);
        return ApiResponse.<Void>builder()
                .message("Deleted image successfully")
                .build();
    }
	@GetMapping
	ApiResponse<List<ProductResponse>> getProducts(){
		return ApiResponse.<List<ProductResponse>>builder()
				.result(productService.getProducts())
				.build();
	}

	@GetMapping("/filter")
	ApiResponse<ProductPageResponse> filterProducts(
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
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		PageRequest pageable = PageRequest.of(page, size, sort);
		return ApiResponse.<ProductPageResponse>builder()
				.result(productService.filterProducts(
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
						minPrice,
						maxPrice,
						status,
						pageable))
				.build();
	}

	@GetMapping("/{productId}/variants")
	ApiResponse<ProductVariantPageResponse> getVariantsByProductId(
			@PathVariable String productId,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String colorName,
			@RequestParam(required = false) String frameFinish,
			@RequestParam(required = false) String sizeLabel,
			@RequestParam(required = false) Integer lensWidthMm,
			@RequestParam(required = false) Integer bridgeWidthMm,
			@RequestParam(required = false) Integer templeLengthMm,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) ProductVariantStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {
		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		PageRequest pageable = PageRequest.of(page, size, sort);
		return ApiResponse.<ProductVariantPageResponse>builder()
				.result(productVariantService.filterVariants(
						q,
						productId,
						colorName,
						frameFinish,
						sizeLabel,
						lensWidthMm,
						bridgeWidthMm,
						templeLengthMm,
						minPrice,
						maxPrice,
						status,
						pageable))
				.build();
	}
	@PostMapping(value = "/{productId}/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
	public ApiResponse<ProductResponse> uploadModel(
			@PathVariable String productId,
			@RequestParam("file") MultipartFile file) throws IOException {
		return ApiResponse.<ProductResponse>builder()
				.result(productService.uploadProductModel(productId, file))
				.message("3D model uploaded successfully")
				.build();
	}


}
