package com.glassystem.optics.controller.order;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderPageResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Slf4j
@RestController
@RequestMapping("/management/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order Management", description = "Administrative endpoints for managing and auditing all customer orders")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('SALE') or hasRole('OPERATION')")
public class ManagementOrderController {

    OrderService orderService;



    @GetMapping("/cancelled/paid")
    @Operation(summary = "Get cancelled orders that already have successful payment")
    public ApiResponse<OrderPageResponse> getCancelledPaidOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<OrderPageResponse>builder()
                .result(orderService.getCancelledPaidOrders(pageable))
                .build();
    }


//    @PostMapping("/{orderId}/stock-arrived")
//    public ApiResponse<OrderResponse> stockArrived(
//            @PathVariable String orderId) {
//
//        return ApiResponse.<OrderResponse>builder()
//                .result(orderService.markStockArrived(orderId))
//                .build();
//    }


    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details with combo info", description = "Provides full details of a specific order including items, prescriptions, and applied combo discount info")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable("orderId") String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderDetailWithCombo(orderId))
                .build();
    }

    @GetMapping
    @Operation(summary = "Filter orders by status", description = "Retrieves a list of orders based on a specific OrderStatus (e.g., PENDING, PROCESSING)")
    public ApiResponse<OrderPageResponse> getOrdersByStatus(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<OrderPageResponse>builder()
                .result(orderService.getOrdersByStatus(status, pageable))
                .build();
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders placed by a specific customer")
    public ApiResponse<OrderPageResponse> getOrdersByCustomerId(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<OrderPageResponse>builder()
                .result(orderService.getOrdersByCustomerId(customerId, pageable))
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
