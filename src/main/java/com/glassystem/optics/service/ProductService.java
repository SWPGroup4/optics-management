package com.glassystem.optics.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.glassystem.optics.dto.request.ProductCreateRequest;
import com.glassystem.optics.dto.request.ProductUpsertRequest;
import com.glassystem.optics.dto.response.ProductImageResponse;
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
	ProductRepository productRepository;
	ProductMapper productMapper;
    FileStorageService fileStorageService;
    ProductImageRepository  productImageRepository;

	public ProductResponse create(ProductCreateRequest request) {

        productRepository.findByNameAndBrand(request.getName(),request.getBrand()).ifPresent(product
                -> {throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTED);});

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

    @Transactional
    public List<ProductImageResponse> uploadProductImages(String productId,
            List<MultipartFile> files
    ) throws IOException {

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
                            .build()
            );
        }

        return responses;
    }
    public  void deleteProductImage(String imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(()-> new AppException(ErrorCode.IMAGE_NOT_FOUND));

        fileStorageService.deleteFileByKey(productImage.getImageUrl());
        productImageRepository.delete(productImage);
    }
}
