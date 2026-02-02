package com.glassystem.optics.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductDetailResponse;
import com.glassystem.optics.dto.response.ProductImageResponse;
import com.glassystem.optics.dto.response.ProductPageResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductImage;

import com.glassystem.optics.enums.ProductCategory;
import com.glassystem.optics.enums.ProductStatus;
import com.glassystem.optics.enums.ProductVariantStatus;
import com.glassystem.optics.enums.S3ImageName;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductMapper;
import com.glassystem.optics.mapper.ProductVariantMapper;
import com.glassystem.optics.repository.ProductImageRepository;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.repository.ProductVariantRepository;
import com.glassystem.optics.specification.ProductSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;
    FileStorageService fileStorageService;
    ProductImageRepository productImageRepository;
    ProductVariantRepository productVariantRepository;
    ProductVariantMapper productVariantMapper;

    public ProductResponse create(ProductCreateRequest request) {
        Product product = productMapper.toProduct(request);
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    public ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toProductResponse(product);
    }

    public ProductResponse update(String id, ProductUpsertRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
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
        return productRepository.findAll().stream().map(productMapper::toProductResponse).toList();
    }

    @Transactional
    public List<ProductImageResponse> uploadProductImages(String productId,
            List<MultipartFile> files) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        int currentImageCount = product.getImageUrl().size();
        if (currentImageCount + files.size() > 5) {
            throw new AppException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        List<ProductImageResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            String url = fileStorageService.uploadFile(file, S3ImageName.PRODUCT);

            ProductImage productImage = ProductImage.builder()
                    .imageUrl(url)
                    .product(product)
                    .build();

            productImage = productImageRepository.save(productImage);

            responses.add(
                    ProductImageResponse.builder()
                            .id(productImage.getId())
                            .imageUrl(productImage.getImageUrl())
                            .build());
        }

        return responses;
    }

    public void deleteProductImage(String imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND));

        fileStorageService.deleteFileByKey(productImage.getImageUrl());
        productImageRepository.delete(productImage);
    }

    public ProductPageResponse filterProducts(
            String q,
            String brand,
            String category,
            String frameType,
            String gender,
            String shape,
            String frameMaterial,
            String hingeType,
            String nosePadType,
            BigDecimal minWeightGram,
            BigDecimal maxWeightGram,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            Pageable pageable) {
        Page<Product> page = productRepository.findAll(
                ProductSpecifications.build(
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
                        status),
                pageable);

        return ProductPageResponse.builder()
                .items(page.getContent().stream().map(productMapper::toProductResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * API-06: Lấy chi tiết Product (Product + Variant + Image).
     *
     * Note:
     * - Trả về dữ liệu đủ cho trang chi tiết sản phẩm.
     * - Ảnh và variant được query riêng thay vì rely vào lazy-loading để tránh lỗi LazyInitialization
     *   khi controller không mở transaction.
     */
    public ProductDetailResponse getPublicProductDetail(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImageResponse> images = productImageRepository.findAllByProduct_Id(productId)
                .stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .toList();

        List<ProductVariantResponse> variants = productVariantRepository
                .findAllByProduct_IdAndStatus(productId, ProductVariantStatus.ACTIVE)
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();

        return ProductDetailResponse.builder()
                .product(productMapper.toProductResponse(product))
                .images(images)
                .variants(variants)
                .build();
    }

    /**
     * API-01: Gợi ý sản phẩm tương tự.
     *
     * Rule:
     * - Cùng category
     * - Cùng gender
     * - Khác product id hiện tại
     */
    public List<ProductResponse> getSimilarProducts(String productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductCategory category = product.getCategory();
        String gender = product.getGender();
        if (category == null || gender == null || gender.isBlank()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));
        PageRequest pageable = PageRequest.of(0, safeLimit, Sort.by("name").ascending());

        return productRepository
                .findByCategoryAndGenderAndIdNotAndStatus(category, gender, productId, ProductStatus.ACTIVE, pageable)
                .stream()
                .map(productMapper::toProductResponse)
                .toList();
    }
}