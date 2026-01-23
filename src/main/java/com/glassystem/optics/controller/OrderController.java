package com.glassystem.optics.controller;

import com.glassystem.optics.dto.request.OrderCreationRequest;
import com.glassystem.optics.dto.request.PrescriptionRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderController {
    final OrderService orderService;

    /* ===================== CREATE ===================== */

    @PostMapping
    ApiResponse<OrderResponse> createOrder(
            @RequestBody OrderCreationRequest request
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request))
                .build();
    }

    /* ===================== READ ===================== */

    // Customer: xem đơn của mình
    @GetMapping("/me")
    ApiResponse<List<OrderResponse>> getMyOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrders())
                .build();
    }

    // Manager / Admin: xem tất cả
    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrders())
                .build();
    }

    // Sale / Admin: xem chi tiết
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('SALE') or hasRole('ADMIN')")
    ApiResponse<OrderResponse> getOrderById(
            @PathVariable String orderId
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderById(orderId))
                .build();
    }

    /* ===================== PRODUCTION FLOW ===================== */

    // Sale xác nhận đơn
    @PutMapping("/{orderId}/verify")
    @PreAuthorize("hasRole('SALE') or hasRole('ADMIN')")
    ApiResponse<OrderResponse> verifyOrder(
            @PathVariable String orderId,
            @RequestParam boolean isValid
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.verifyOrder(orderId,  isValid))
                .build();
    }

    @PutMapping("/items/{orderItemId}/prescription")
    @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<OrderResponse> updatePrescription(@PathVariable String orderItemId,
                                                  @RequestBody PrescriptionRequest prescriptionRequest) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updatePrescription(orderItemId, prescriptionRequest))
                .build();
    }

    // Operation: danh sách đang sản xuất
    @GetMapping("/in-production")
    @PreAuthorize("hasRole('OPERATION')")
    ApiResponse<List<OrderResponse>> getOrdersInProduction() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersInProduction())
                .build();
    }

    // Bắt đầu sản xuất
    @PutMapping("/{orderId}/start-production")
    @PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
    ApiResponse<OrderResponse> startProduction(
            @PathVariable String orderId
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.startProduction(orderId))
                .build();
    }

    // Hoàn tất sản xuất
    @PutMapping("/{orderId}/finish-production")
    @PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
    ApiResponse<OrderResponse> finishProduction(
            @PathVariable String orderId
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.finishProduction(orderId))
                .build();
    }

    // Danh sách đã sản xuất xong
    @GetMapping("/finished-production")
    ApiResponse<List<OrderResponse>> getOrdersFinishProduction() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersFinishProduction())
                .build();
    }

    /* ===================== SHIPPING ===================== */

    @PutMapping("/{orderId}/ship")
    ApiResponse<OrderResponse> shipOrder(
            @PathVariable String orderId
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.shipOrder(orderId))
                .build();
    }

    @GetMapping("/shipped")
    ApiResponse<List<OrderResponse>> getOrdersShipped() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersShipped())
                .build();
    }

    /* ===================== CANCEL ===================== */

    @PutMapping("/{orderId}/cancel")
    ApiResponse<OrderResponse> cancelOrder(
            @PathVariable String orderId
    ) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.cancelOrder(orderId))
                .build();
    }

    @GetMapping("/cancelled")
    ApiResponse<List<OrderResponse>> getOrdersCancelled() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersCancelled())
                .build();
    }

    /* ===================== DELETE ===================== */

    @DeleteMapping("/{orderId}")
    ApiResponse<String> deleteOrder(
            @PathVariable String orderId
    ) {
        orderService.deleteOrder(orderId);
        return ApiResponse.<String>builder()
                .result("Order has been deleted and inventory restored successfully")
                .build();
    }

}
