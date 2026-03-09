package com.glassystem.optics.controller.order;

import com.glassystem.optics.dto.request.ShipOrdersRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ship/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Production Management", description = "Endpoints for technical staff to manage lens grinding and assembly workflows")
@PreAuthorize("hasRole('SHIPPER') or hasRole('ADMIN')")
public class ShipOrderController {

    OrderService orderService;


    @PostMapping("/{orderId}/accept")
    public ApiResponse<OrderResponse> acceptOrder(
            @PathVariable("orderId") String orderId,
            String shipperId) {

         shipperId  = SecurityContextHolder.getContext().getAuthentication().getName();

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.acceptOrder(orderId, shipperId))
                .build();
    }

//    @PutMapping("/{orderId}/finish")
//    @Operation(summary = "Complete order production", description = "Marks an entire order as PRODUCED once all items are ready")
//    public ApiResponse<OrderResponse> finishProduction(@PathVariable("orderId") String orderId) {
//        return ApiResponse.<OrderResponse>builder()
//                .result(orderService.finishProduction(orderId))
//                .message("Order production finalized successfully")
//                .build();
//    }
//
//    @PutMapping("/items/{orderItemId}/status")
//    @Operation(summary = "Update item-level status", description = "Updates status for individual items (e.g., lens grinding finished)")
//    public ApiResponse<OrderResponse> updateItemProductionStatus(
//            @PathVariable("orderItemId") String orderItemId,
//            @RequestParam("status") OrderItemStatus status) {
//        return ApiResponse.<OrderResponse>builder()
//                .result(orderService.updateOrderItemProductionStatus(orderItemId, status))
//                .build();
//    }
//
//    @PutMapping("/orders/{orderId}/prepared")
//    public ApiResponse<OrderResponse>  markOrderAsPrepared(
//            @PathVariable("orderId")
//            String orderId) {
//        return ApiResponse.<OrderResponse>builder()
//                .result(orderService.markAsPrepared(orderId))
//                .build();
//    }
//
//    @PutMapping("/ship")
//    @Operation(summary = "Mark order as shipped", description = "Confirm that the order has been handed over to the courier service")
//    public ApiResponse<List<OrderResponse>> markAsShipped(@RequestBody ShipOrdersRequest request) {
//        return ApiResponse.<List<OrderResponse>>builder()
//                .result(orderService.markAsReadyToShip(request.getOrderIds()))
//                .message("Order status updated to SHIPPED")
//                .build();
//    }


}