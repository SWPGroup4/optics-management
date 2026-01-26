package com.glassystem.optics.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.glassystem.optics.dto.request.InventoryUpdateRequest;
import com.glassystem.optics.dto.request.ProductVariantRequest;
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
import jdk.dynalink.linker.LinkerServices;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
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
                .findByProductIdAndColorNameAndSizeLabel
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
}
