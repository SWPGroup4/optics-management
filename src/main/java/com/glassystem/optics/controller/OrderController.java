package com.glassystem.optics.controller;

import com.glassystem.optics.dto.request.OrderCreationRequest;
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

    @PostMapping()
    ApiResponse<OrderResponse> createOrder (@RequestBody OrderCreationRequest request){
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request))
                .build();

    }
    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse <List<OrderResponse>>getAllOrders(){
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrders())
                .build();
    }

    @GetMapping("/me")
    ApiResponse<List<OrderResponse>> getMyOrders(){
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrders())
                .build();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<OrderResponse> getOrderById(@PathVariable("orderId") String id){
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderById(id))
                .build();
    }

}
