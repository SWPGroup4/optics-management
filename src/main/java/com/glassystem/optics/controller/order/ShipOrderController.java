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
@PreAuthorize("hasRole('SHIPPER') or hasRole('ADMIN')")
public class ShipOrderController {

    OrderService orderService;


    @PatchMapping("/accept")
    public ApiResponse<List<OrderResponse>> acceptOrders(
            @RequestBody ShipOrdersRequest request) {

        String shipperId = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.acceptOrders(request.getOrderIds(), shipperId))
                .build();
    }

    @PatchMapping("/{orderId}/start-delivery")
    public ApiResponse<OrderResponse> startDelivery(
            @PathVariable("orderId") String orderId,
            String shipperId) {

        shipperId  = SecurityContextHolder.getContext().getAuthentication().getName();

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.startDelivery(orderId,  shipperId))
                .build();
    }

    @PatchMapping("{orderId}/confirm-delivered")
    public ApiResponse<OrderResponse> updateItemProductionStatus(
            @PathVariable("orderId") String orderId,
            String shipperId) {

        shipperId  = SecurityContextHolder.getContext().getAuthentication().getName();

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.confirmDelivered(orderId, shipperId))
                .build();
    }



}