package com.glassystem.optics.controller.order;

import java.util.List;

import com.glassystem.optics.dto.request.ShipOrdersRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.enums.OrderItemStatus;
import com.glassystem.optics.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/production/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Production Management", description = "Endpoints for technical staff to manage lens grinding and assembly workflows")
@PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
public class ProductionOrderController {

    OrderService orderService;


    @PutMapping("/{orderId}/start")
    @Operation(summary = "Start order production", description = "Initializes the production phase for an order, changing status to PROCESSING")
    public ApiResponse<OrderResponse> startProduction(@PathVariable("orderId") String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.startProduction(orderId))
                .build();
    }

    @PutMapping("/{orderId}/finish")
    @Operation(summary = "Complete order production", description = "Marks an entire order as PRODUCED once all items are ready")
    public ApiResponse<OrderResponse> finishProduction(@PathVariable("orderId") String orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.finishProductionOrder(orderId))
                .message("Order production finalized successfully")
                .build();
    }

    @PutMapping("/items/{orderItemId}/status")
    @Operation(summary = "Update item-level status", description = "Updates status for individual items (e.g., lens grinding finished)")
    public ApiResponse<OrderResponse> updateItemProductionStatus(
            @PathVariable("orderItemId") String orderItemId,
            @RequestParam("status") OrderItemStatus status) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updateOrderItemProductionStatus(orderItemId, status))
                .build();
    }



    @PutMapping("/ready-to-ship")
    public ApiResponse<List<OrderResponse>> markAsShipped(@RequestBody ShipOrdersRequest request) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.markAsReadyToShip(request.getOrderIds()))
                .build();
    }


}