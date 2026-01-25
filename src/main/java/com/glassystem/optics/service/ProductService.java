package com.glassystem.optics.service;

import java.math.BigDecimal;
import java.util.List;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.enums.ProductStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductMapper;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.specification.ProductSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
	ProductRepository productRepository;
	ProductMapper productMapper;

	public ProductResponse create(ProductCreateRequest request) {
		Product product = productMapper.toProduct(request);
		product = productRepository.save(product);
		return productMapper.toProductResponse(product);
	}

	public ProductResponse getById(String id) {
		Product product = productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
		return productMapper.toProductResponse(product);
	}

	public ProductResponse update(String id, ProductUpsertRequest request) {
		Product product = productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
		productMapper.updateProduct(product, request);
		product = productRepository.save(product);
		return productMapper.toProductResponse(product);
	}

	public void delete(String id) {
		if (!productRepository.existsById(id)) {
			throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		productRepository.deleteById(id);
	}

	public List<ProductResponse> getProducts() {
		return  productRepository.findAll().stream().map(productMapper::toProductResponse).toList();
    }
}
