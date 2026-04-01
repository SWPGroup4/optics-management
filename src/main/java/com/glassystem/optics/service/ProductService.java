package com.glassystem.optics.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductImageResponse;
import com.glassystem.optics.dto.response.ProductPageResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.entity.ProductImage;
import com.glassystem.optics.enums.ProductStatus;
import com.glassystem.optics.enums.S3ImageName;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductMapper;
import com.glassystem.optics.repository.ProductImageRepository;
import com.glassystem.optics.repository.ProductRepository;
import com.glassystem.optics.specification.ProductSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public ProductResponse create(ProductCreateRequest request, List<MultipartFile> files, MultipartFile modelFile) throws IOException {
        Product product = productMapper.toProduct(request);

        if (modelFile != null && !modelFile.isEmpty()) {
            String modelUrl = fileStorageService.uploadFile(modelFile, S3ImageName.MODEL);
            product.setModelUrl(modelUrl);
        }

        product = productRepository.save(product);

        if (files != null && !files.isEmpty()) {
            if (files.size() > 5) {
                throw new AppException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
            }
            List<ProductImage> images = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileStorageService.uploadFile(file, S3ImageName.PRODUCT);
                ProductImage productImage = ProductImage.builder()
                        .imageUrl(url)
                        .product(product)
                        .build();
                productImage = productImageRepository.save(productImage);
                images.add(productImage);
            }
            product.setImageUrl(images);
        }

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

    @Transactional
    public void delete(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsDeleted(true);
        // Soft delete all variants of this product
        if (product.getVariants() != null) {
            for (var variant : product.getVariants()) {
                variant.setIsDeleted(true);
            }
        }
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return productRepository.findAllWithImages().stream().map(productMapper::toProductResponse).toList();
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

    @Transactional
    public ProductResponse uploadProductModel(String productId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getModelUrl() != null && !product.getModelUrl().isBlank()) {
            fileStorageService.deleteFileByKey(product.getModelUrl());
        }

        String modelUrl = fileStorageService.uploadFile(file, S3ImageName.MODEL);
        product.setModelUrl(modelUrl);
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse deleteProductModel(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getModelUrl() != null && !product.getModelUrl().isBlank()) {
            fileStorageService.deleteFileByKey(product.getModelUrl());
        }

        product.setModelUrl(null);
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
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

}