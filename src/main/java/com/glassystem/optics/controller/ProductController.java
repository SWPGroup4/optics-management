package com.glassystem.optics.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/{productId}/images")
    public ApiResponse<ProductResponse> uploadImages(
            @PathVariable String productId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.uploadProductImages(productId, files))
                .message("Uploaded successfully")
                .build();
    }

    @DeleteMapping("/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable String imageId) {
        productService.deleteProductImage(imageId);
        return ApiResponse.<Void>builder()
                .message("Deleted image successfully")
                .build();
    }

	@DeleteMapping("/{id}")
	ApiResponse<Void> delete(@PathVariable String id) {
		productService.delete(id);
		return ApiResponse.<Void>builder().build();
	}

	@GetMapping
    ApiResponse<List<ProductResponse>> getProducts(){
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getProducts())
                .build();
    }
}
