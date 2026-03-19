package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.FeedbackRequest;
import com.glassystem.optics.dto.request.FeedbackUpdateRequest;
import com.glassystem.optics.dto.response.FeedbackResponse;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.S3ImageName;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.FeedbackMapper;
import com.glassystem.optics.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class FeedbackService {

    final FeedbackRepository feedbackRepository;
    final OrderRepository orderRepository;
    final ProductRepository productRepository;
    final UserRepository userRepository;
    final FeedbackMapper feedbackMapper;
    final FileStorageService fileStorageService;

    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, List<MultipartFile> images) throws IOException {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Orders order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.FEEDBACK_ORDER_NOT_COMPLETED);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean productInOrder = order.getItems().stream()
                .anyMatch(item -> item.getProductVariant() != null
                        && item.getProductVariant().getProduct() != null
                        && item.getProductVariant().getProduct().getId().equals(request.getProductId()));

        if (!productInOrder) {
            throw new AppException(ErrorCode.FEEDBACK_PRODUCT_NOT_IN_ORDER);
        }

        if (feedbackRepository.existsByOrderIdAndProductIdAndCustomerId(
                request.getOrderId(), request.getProductId(), userId)) {
            throw new AppException(ErrorCode.FEEDBACK_ALREADY_EXISTS);
        }

        Feedback feedback = Feedback.builder()
                .order(order)
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (images != null && !images.isEmpty()) {
            if (images.size() > 5) {
                throw new AppException(ErrorCode.FEEDBACK_IMAGE_LIMIT_EXCEEDED);
            }
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String url = fileStorageService.uploadFile(image, S3ImageName.FEEDBACK);
                    imageUrls.add(url);
                }
            }
            feedback.setImageUrls(imageUrls);
        }

        feedback = feedbackRepository.save(feedback);
        log.info("Feedback created: feedbackId={}, orderId={}, productId={}, customerId={}",
                feedback.getId(), request.getOrderId(), request.getProductId(), userId);

        return feedbackMapper.toFeedbackResponse(feedback);
    }

    @Transactional
    public FeedbackResponse updateFeedback(String feedbackId, FeedbackUpdateRequest request, List<MultipartFile> images) throws IOException {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getRating() != null) {
            feedback.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            feedback.setComment(request.getComment());
        }

        if (images != null && !images.isEmpty()) {
            if (images.size() > 5) {
                throw new AppException(ErrorCode.FEEDBACK_IMAGE_LIMIT_EXCEEDED);
            }
            for (String oldUrl : feedback.getImageUrls()) {
                fileStorageService.deleteFileByKey(oldUrl);
            }
            List<String> newImageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String url = fileStorageService.uploadFile(image, S3ImageName.FEEDBACK);
                    newImageUrls.add(url);
                }
            }
            feedback.setImageUrls(newImageUrls);
        }

        feedback.setUpdatedAt(LocalDateTime.now());
        feedback = feedbackRepository.save(feedback);

        log.info("Feedback updated: feedbackId={}", feedbackId);
        return feedbackMapper.toFeedbackResponse(feedback);
    }

    @Transactional
    public void deleteFeedback(String feedbackId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        for (String imageUrl : feedback.getImageUrls()) {
            fileStorageService.deleteFileByKey(imageUrl);
        }

        feedbackRepository.delete(feedback);
        log.info("Feedback deleted: feedbackId={}", feedbackId);
    }

    public List<FeedbackResponse> getFeedbacksByProductId(String productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return feedbackMapper.toFeedbackResponseList(feedbackRepository.findByProductId(productId));
    }

    public List<FeedbackResponse> getMyFeedbacks() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return feedbackMapper.toFeedbackResponseList(feedbackRepository.findByCustomerId(userId));
    }

    public List<FeedbackResponse> getFeedbacksByOrderId(String orderId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomer().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return feedbackMapper.toFeedbackResponseList(feedbackRepository.findByOrderId(orderId));
    }

    public FeedbackResponse getFeedbackById(String feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));
        return feedbackMapper.toFeedbackResponse(feedback);
    }
}
