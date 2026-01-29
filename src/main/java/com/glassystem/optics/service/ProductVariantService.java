package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.ProductVariantPageResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductVariantMapper;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.repository.ProductVariantRepository;
import com.glassystem.optics.specification.ProductVariantSpecifications;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantService {
	ProductVariantRepository productVariantRepository;
	ProductRepository productRepository;
	ProductVariantMapper productVariantMapper;

	public ProductVariantResponse create(@Valid ProductVariantRequest request) {
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

		ProductVariant variant = productVariantMapper.toEntity(request);
		variant.setProduct(product);
		variant = productVariantRepository.save(variant);
		return productVariantMapper.toResponse(variant);
	}

	public ProductVariantResponse getById(String id) {
		ProductVariant variant = productVariantRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
		return productVariantMapper.toResponse(variant);
	}

	public ProductVariantResponse update(String id, @Valid ProductVariantRequest request) {
		ProductVariant variant = productVariantRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

		productVariantMapper.updateEntity(variant, request);
		variant.setProduct(product);
		variant = productVariantRepository.save(variant);
		return productVariantMapper.toResponse(variant);
	}

	public void delete(String id) {
		if (!productVariantRepository.existsById(id)) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
		}
		productVariantRepository.deleteById(id);
	}

	public Page<ProductVariantResponse> getVariants(
			String q,
			String productId,
			String colorName,
			String frameFinish,
			String sizeLabel,
			Integer lensWidthMm,
			Integer bridgeWidthMm,
			Integer templeLengthMm,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			ProductVariantStatus status,
			Pageable pageable) {
		var spec = ProductVariantSpecifications.build(
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
				status);

		return productVariantRepository.findAll(spec, pageable).map(productVariantMapper::toResponse);
	}

	public ProductVariantPageResponse filterVariants(
			String q,
			String productId,
			String colorName,
			String frameFinish,
			String sizeLabel,
			Integer lensWidthMm,
			Integer bridgeWidthMm,
			Integer templeLengthMm,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			ProductVariantStatus status,
			Pageable pageable) {

		var spec = ProductVariantSpecifications.build(
				q, productId, colorName, frameFinish, sizeLabel,
				lensWidthMm, bridgeWidthMm, templeLengthMm,
				minPrice, maxPrice, status);

		Page<ProductVariant> variantPage = productVariantRepository.findAll(spec, pageable);

		return ProductVariantPageResponse.builder()
				.items(variantPage.getContent().stream()
						.map(productVariantMapper::toResponse)
						.toList())
				.page(variantPage.getNumber())
				.size(variantPage.getSize())
				.totalElements(variantPage.getTotalElements())
				.totalPages(variantPage.getTotalPages())
				.build();
	}
}
