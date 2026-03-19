package com.glassystem.optics.controller.feedback;

import com.glassystem.optics.dto.request.FeedbackRequest;
import com.glassystem.optics.dto.request.FeedbackUpdateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.FeedbackResponse;
import com.glassystem.optics.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Feedback Management", description = "Endpoints for customers to submit and manage product feedback after order completion")
public class FeedbackController {

    FeedbackService feedbackService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Submit feedback", description = "Customer submits feedback with rating, comment and optional images for a product in a completed order")
    public ApiResponse<FeedbackResponse> createFeedback(
            @RequestPart("feedback") @Valid FeedbackRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        return ApiResponse.<FeedbackResponse>builder()
                .result(feedbackService.createFeedback(request, images))
                .message("Feedback submitted successfully!")
                .build();
    }

    @PutMapping(value = "/{feedbackId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Update feedback", description = "Customer updates their existing feedback")
    public ApiResponse<FeedbackResponse> updateFeedback(
            @PathVariable String feedbackId,
            @RequestPart("feedback") @Valid FeedbackUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
        return ApiResponse.<FeedbackResponse>builder()
                .result(feedbackService.updateFeedback(feedbackId, request, images))
                .message("Feedback updated successfully!")
                .build();
    }

    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Delete feedback", description = "Customer deletes their own feedback")
    public ApiResponse<Void> deleteFeedback(@PathVariable String feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return ApiResponse.<Void>builder()
                .message("Feedback deleted successfully!")
                .build();
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get feedbacks by product", description = "Get all feedbacks for a specific product (public)")
    public ApiResponse<List<FeedbackResponse>> getFeedbacksByProduct(@PathVariable String productId) {
        return ApiResponse.<List<FeedbackResponse>>builder()
                .result(feedbackService.getFeedbacksByProductId(productId))
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Get my feedbacks", description = "Get all feedbacks submitted by the current customer")
    public ApiResponse<List<FeedbackResponse>> getMyFeedbacks() {
        return ApiResponse.<List<FeedbackResponse>>builder()
                .result(feedbackService.getMyFeedbacks())
                .build();
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(summary = "Get feedbacks by order", description = "Get all feedbacks for a specific order")
    public ApiResponse<List<FeedbackResponse>> getFeedbacksByOrder(@PathVariable String orderId) {
        return ApiResponse.<List<FeedbackResponse>>builder()
                .result(feedbackService.getFeedbacksByOrderId(orderId))
                .build();
    }

    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get feedback detail", description = "Get detail of a specific feedback")
    public ApiResponse<FeedbackResponse> getFeedbackById(@PathVariable String feedbackId) {
        return ApiResponse.<FeedbackResponse>builder()
                .result(feedbackService.getFeedbackById(feedbackId))
                .build();
    }
}
