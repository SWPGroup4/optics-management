package com.glassystem.optics.controller.refund;


import com.glassystem.optics.dto.request.BankInfoRequest;
import com.glassystem.optics.dto.request.RefundBatchRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class RefundController {

    private final RefundService refundService;


    @GetMapping("/affected-orders/{variantId}")
    public ApiResponse<List<RefundResponse>> getAffectedOrders(
            @PathVariable String variantId) {
        return ApiResponse.<List<RefundResponse>>builder()
                .result(refundService.getAffectedOrders(variantId))
                .build();
    }


    @PostMapping("/create/{orderId}")
    public ApiResponse<Void> createRefund(@PathVariable String orderId){
        refundService.createRefundRequest(orderId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/create-batch")
    public ApiResponse<RefundResponse> createRefundBatch(
            @RequestBody RefundBatchRequest request){

        return ApiResponse.<RefundResponse>builder()
                .result(refundService.createRefundRequests(request.getOrderIds()))
                .build();
    }

    @PostMapping("/create-by-variant/{variantId}")
    public ApiResponse<Void> createRefundByVariant(
            @PathVariable String variantId) {
        refundService.createRefundByVariant(variantId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/bank-info/{refundId}")
    public ApiResponse<Void> submitBankInfo(
            @PathVariable String refundId,
            @RequestBody BankInfoRequest request){

        refundService.submitBankInfo(refundId,request);

        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/ready")
    public ApiResponse<List<RefundResponse>> getReadyRefunds(){

        return ApiResponse.<List<RefundResponse>>builder()
                .result(refundService.getReadyRefunds())
                .build();
    }

    @PostMapping("/complete/{refundId}")
    public ApiResponse<Void> completeRefund(
            @PathVariable String refundId,
            @RequestParam String managerId){

        refundService.completeRefund(refundId,managerId);

        return ApiResponse.<Void>builder().build();
    }
}
