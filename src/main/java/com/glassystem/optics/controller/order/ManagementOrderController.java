package com.glassystem.optics.controller.order;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/management/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order Management", description = "Administrative endpoints for managing and auditing all customer orders")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class ManagementOrderController {

    OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all system orders", description = "Retrieves a comprehensive list of all orders across the system")
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrders())
                .build();
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details", description = "Provides full details of a specific order including items and prescriptions")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable("orderId") String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderById(orderId))
                .build();
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter orders by status", description = "Retrieves a list of orders based on a specific OrderStatus (e.g., PENDING, PROCESSING)")
    public ApiResponse<List<OrderResponse>> getOrdersByStatus(@RequestParam("status") OrderStatus status) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersByStatus(status))
                .build();
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete an order", description = "Permanently removes an order from the system. Restricted to ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteOrder(@PathVariable("orderId") String orderId) {
        orderService.deleteOrder(orderId);
        return ApiResponse.<Void>builder()
                .message("Order deleted successfully from system logs")
                .build();
    }
}