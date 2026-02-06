package com.glassystem.optics.controller.order;

import java.io.IOException;
import java.util.List;

import com.glassystem.optics.dto.request.OrderItemCreationRequest;
import com.glassystem.optics.dto.response.PaymentRequirementResponse;
import com.glassystem.optics.enums.OrderItemStatus;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.enums.PaymentMethod;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.glassystem.optics.dto.request.OrderCreationRequest;
import com.glassystem.optics.dto.request.OrderUpdateRequest;
import com.glassystem.optics.dto.request.PrescriptionRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.dto.response.PrescriptionResponse;
import com.glassystem.optics.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Customer Order Management", description = "Endpoints for customers to manage their own orders and prescriptions")
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
public class CustomerOrderController {

    OrderService orderService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Place a new order", description = "Allows a customer to create a new order with multiple items")
    public ApiResponse<OrderResponse> createOrder(@RequestPart("orderInfo") @Valid OrderCreationRequest request,
                                                  @RequestParam(value = "OrderItemType", required = true) OrderItemType type,
                                                  @RequestParam(value = "PaymentMethod", required = true) PaymentMethod paymentMethod,
                                                  @RequestPart(value = "prescriptionImage", required = false) MultipartFile file) throws IOException {


        if (paymentMethod != null) {
            request.setPaymentMethod(paymentMethod);
        }
        for(OrderItemCreationRequest item : request.getItems()) {
            item.setOrderItemType(type);
        }


        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request, file))
                .build();
    }

    @PutMapping(value = "/items/{orderItemId}/prescription-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload prescription image", description = "Upload a photo of the medical prescription for a specific item")
    public ApiResponse<PrescriptionResponse> uploadPrescriptionImage(
            @PathVariable String orderItemId,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ApiResponse.<PrescriptionResponse>builder()
                .result(orderService.uploadPrescriptionImage(orderItemId, file))
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get my order history", description = "Retrieves all orders placed by the current logged-in customer")
    public ApiResponse<List<OrderResponse>> getMyOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrders())
                .build();
    }

    @GetMapping("/me/cancelled")
    @Operation(summary = "Get my cancelled orders", description = "Retrieves only the cancelled orders of the current customer")
    public ApiResponse<List<OrderResponse>> getMyCancelledOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyCancelledOrders())
                .build();
    }

    @PutMapping("/{orderId}")
    @Operation(summary = "Update order details", description = "Allows updating delivery address or prescription details while order is PENDING")
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable String orderId,
            @RequestBody @Valid OrderUpdateRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updateOrder(orderId, request))
                .build();
    }

    @PutMapping("/items/{orderItemId}/prescription")
    @Operation(summary = "Update item prescription", description = "Updates prescription data for a specific glass item in an order")
    public ApiResponse<PrescriptionResponse> updatePrescription(
            @PathVariable String orderItemId,
            @RequestBody @Valid PrescriptionRequest request) {
        return ApiResponse.<PrescriptionResponse>builder()
                .result(orderService.updatePrescription(orderItemId, request))
                .build();
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Allows customer to cancel an order if it has not been confirmed yet")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.cancelOrder(orderId))
                .message("Order cancelled successfully")
                .build();
    }

    @PutMapping("/{orderId}/complete")
    @Operation(summary = "Confirm order receipt", description = "Customer confirms they have received the order successfully")
    public ApiResponse<OrderResponse> completeOrder(@PathVariable String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.completeOrder(orderId))
                .message("Order completed successfully. Thank you!")
                .build();
    }


    @GetMapping("/{orderId}/payment-requirement")
    @Operation(summary = "Kiểm tra yêu cầu đặt cọc",
            description = "Dùng để xác định số tiền khách cần trả trước dựa trên các loại sản phẩm trong giỏ hàng")
    public ApiResponse<PaymentRequirementResponse> validatePayment(@PathVariable String orderId) {
        return ApiResponse.<PaymentRequirementResponse>builder()
                .result(orderService.getPaymentRequirement(orderId))
                .build();
    }
}