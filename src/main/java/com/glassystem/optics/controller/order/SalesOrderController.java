package com.glassystem.optics.controller.order;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/sales/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Sales Operations", description = "Endpoints for sales staff to verify, reject, and dispatch orders")
@PreAuthorize("hasRole('SALE') or hasRole('ADMIN')")
public class SalesOrderController {

    OrderService orderService;

    @PutMapping("/{orderId}/verify")
    @Operation(summary = "Verify order ", description = "Allows sales staff to confirm the validity of a customer's order ")
    public ApiResponse<OrderResponse> verifyOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("isApproved") boolean isApproved) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.verifyOrder(orderId, isApproved))
                .build();
    }

    @PutMapping("/{orderId}/reject")
    @Operation(summary = "Reject order", description = "System-level rejection of an order due to invalid data or fraud detection. Automatically releases reserved inventory.")
    public ApiResponse<OrderResponse> rejectOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.rejectOrder(orderId, reason))
                .message("Order has been rejected and inventory released")
                .build();
    }

    @PutMapping("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped", description = "Confirm that the order has been handed over to the courier service")
    public ApiResponse<OrderResponse> markAsShipped(@PathVariable("orderId") String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markAsShipped(orderId))
                .message("Order status updated to SHIPPED")
                .build();
    }
}