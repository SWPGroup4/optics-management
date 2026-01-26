package com.glassystem.optics.controller.order;

import java.util.List;

import jakarta.validation.Valid;

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

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Customer Order Management", description = "Endpoints for customers to manage their own orders and prescriptions")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order", description = "Allows a customer to create a new order with multiple items")
    public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderCreationRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request))
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
}