package com.glassystem.optics.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.glassystem.optics.dto.request.InventoryUpdateRequest;
import com.glassystem.optics.dto.request.ProductVariantRequest;
import com.glassystem.optics.dto.response.ProductVariantCompareResponse;
import com.glassystem.optics.dto.response.ProductVariantPageResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.entity.Inventory;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductVariant;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductVariantMapper;
import com.glassystem.optics.repository.InventoryRepository;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.repository.ProductVariantRepository;
import com.glassystem.optics.specification.ProductVariantSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantService {
	ProductVariantRepository productVariantRepository;
	ProductRepository productRepository;
	ProductVariantMapper productVariantMapper;
	InventoryRepository inventoryRepository;

	public ProductVariantResponse create(ProductVariantRequest request) {

		Optional<ProductVariant> existingVariant = productVariantRepository
				.findByProduct_IdAndColorNameAndSizeLabel
						(request.getProductId(),request.getColorName(),request.getSizeLabel());

		if(existingVariant.isPresent()) {
			ProductVariant productVariant = existingVariant.get();
			Inventory inventory = inventoryRepository.findByProductVariantId(productVariant.getId())
					.orElseGet(()-> inventoryRepository.save(
							Inventory.builder()
									.productVariant(productVariant)
									.quantity(0)
									.reservedQuantity(0)
									.build()
					));
			inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
			inventoryRepository.save(inventory);

			return productVariantMapper.toResponse(productVariant);
		}

		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

		ProductVariant variant = productVariantMapper.toProductVariant(request);
		variant.setProduct(product);
		variant = productVariantRepository.save(variant);

		Inventory inventory = Inventory.builder()
				.productVariant(variant)
				.quantity(request.getQuantity())
				.reservedQuantity(0)
				.build();
		inventoryRepository.save(inventory);

		return productVariantMapper.toResponse(variant);
	}

	public ProductVariantResponse updateQuantity (InventoryUpdateRequest request){
		Inventory inventory = inventoryRepository.findByProductVariantId(request.getProductVariantId())
				.orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

		int newQuantity = inventory.getQuantity() + request.getChangeAmount();
		inventory.setQuantity(newQuantity);

		if(newQuantity < 0){
			throw new AppException(ErrorCode.OUT_OF_STOCK);
		}
		inventory.setQuantity(newQuantity);
		inventoryRepository.save(inventory);
		return productVariantMapper.toResponse(inventory.getProductVariant());
	}

	public ProductVariantResponse getById(String id) {
		ProductVariant variant = productVariantRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
		return productVariantMapper.toResponse(variant);
	}

	public ProductVariantResponse update(String id, ProductVariantRequest request) {
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

		ProductVariant productVariant = productVariantRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

		Inventory inventory = inventoryRepository
				.findByProductVariantId(id)
				.orElse(null);

		if (inventory == null) {
			productVariantRepository.delete(productVariant);
			return;
		}

		if (inventory.getQuantity() == 0) {
			inventoryRepository.delete(inventory);
			productVariantRepository.delete(productVariant);
			return;
		}

		productVariant.setStatus(ProductVariantStatus.INACTIVE);
		productVariantRepository.save(productVariant);
	}



	public List<ProductVariantResponse> getVariants() {
		return productVariantRepository.findAll().stream().map(productVariantMapper::toResponse).toList();
	}

	public ProductVariantPageResponse filterVariants(
			String search,
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
		Page<ProductVariant> page = productVariantRepository.findAll(
				ProductVariantSpecifications.build(
						search,
						productId,
						colorName,
						frameFinish,
						sizeLabel,
						lensWidthMm,
						bridgeWidthMm,
						templeLengthMm,
						minPrice,
						maxPrice,
						status),
				pageable);

		return ProductVariantPageResponse.builder()
				.items(page.getContent().stream().map(productVariantMapper::toResponse).toList())
				.page(page.getNumber())
				.size(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.build();
	}

	/**
	 * API-04: Tìm Product Variant phù hợp theo thông số mặt kính.
	 *
	 * Rule:
	 * - User nhập lensWidthMm, bridgeWidthMm, templeLengthMm.
	 * - Hệ thống trả về variant ACTIVE có "khoảng cách" nhỏ nhất.
	 * - Khoảng cách ở đây là tổng |lens-lens'| + |bridge-bridge'| + |temple-temple'|.
	 *
	 * Note:
	 * - Nếu truyền thêm productId, chỉ tìm trong variants của product đó.
	 */
	public ProductVariantResponse findNearestVariantBySize(
			Integer lensWidthMm,
			Integer bridgeWidthMm,
			Integer templeLengthMm,
			String productId) {
		if (lensWidthMm == null || lensWidthMm <= 0) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_LENS_WIDTH_INVALID);
		}
		if (bridgeWidthMm == null || bridgeWidthMm <= 0) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_BRIDGE_WIDTH_INVALID);
		}
		if (templeLengthMm == null || templeLengthMm <= 0) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_TEMPLE_LENGTH_INVALID);
		}

		List<ProductVariant> candidates = productVariantRepository.findNearestBySize(
				lensWidthMm,
				bridgeWidthMm,
				templeLengthMm,
				productId,
				ProductVariantStatus.ACTIVE,
				PageRequest.of(0, 1));

		if (candidates.isEmpty()) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
		}

		return productVariantMapper.toResponse(candidates.get(0));
	}

	/**
	 * API-05: So sánh các Product Variant.
	 *
	 * Rule:
	 * - Chỉ cho phép compare các variant thuộc cùng một product.
	 * - Nếu client gửi variantIds không thuộc productId hoặc không tồn tại => báo lỗi.
	 */
	public ProductVariantCompareResponse compareVariants(String productId, List<String> variantIds) {
		if (variantIds == null || variantIds.isEmpty()) {
			throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
		}

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

		List<ProductVariant> variants = productVariantRepository.findAllByIdIn(variantIds);
		Set<String> foundIds = variants.stream().map(ProductVariant::getId).collect(Collectors.toSet());
		for (String id : variantIds) {
			if (!foundIds.contains(id)) {
				throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
			}
		}

		for (ProductVariant v : variants) {
			if (v.getProduct() == null || v.getProduct().getId() == null || !v.getProduct().getId().equals(productId)) {
				throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
			}
		}

		return ProductVariantCompareResponse.builder()
				.productId(product.getId())
				.variants(variants.stream().map(productVariantMapper::toResponse).toList())
				.build();
	}
}
