package com.glassystem.optics.controller.refund;


import com.glassystem.optics.dto.request.BankInfoRequest;
import com.glassystem.optics.dto.request.RefundBatchRequest;
import com.glassystem.optics.dto.response.*;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.entity.Refund;
import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentPurpose;
import com.glassystem.optics.enums.PaymentStatus;
import com.glassystem.optics.enums.RefundStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.repository.PaymentRepository;
import com.glassystem.optics.repository.RefundRepository;
import com.glassystem.optics.service.ProductService;
import com.glassystem.optics.service.ProductVariantService;
import com.glassystem.optics.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('CUSTOMER')")
public class RefundController {

    private final RefundService refundService;



    @PatchMapping("/variant/{variantId}/in-activate")
    public ApiResponse<ProductVariantResponse> inactivateVariant(@PathVariable String variantId){

        return ApiResponse.<ProductVariantResponse>builder()
                .result(refundService.inactivateVariant(variantId))
                .build();
    }


    @GetMapping("/affected-orders/{variantId}")
    public ApiResponse<List<RefundResponse>> getAffectedOrders(
            @PathVariable String variantId) {
        return ApiResponse.<List<RefundResponse>>builder()
                .result(refundService.getAffectedOrders(variantId))
                .build();
    }

    @PostMapping("/create-batch")
    public ApiResponse<List<RefundResponse>> createRefundBatch(
            @RequestBody RefundBatchRequest request){
        return ApiResponse.<List<RefundResponse>>builder()
                .result(refundService.createRefundRequests(request.getOrderIds()))
                .build();
    }



//    @PostMapping("/bank-info/{refundId}")
//    public ApiResponse<RefundBankAccountResponse> submitBankInfo(
//            @PathVariable String refundId,
//            @RequestBody BankInfoRequest request){
//        return ApiResponse.<RefundBankAccountResponse>builder()
//                .result(refundService.submitBankInfo(refundId, request))
//                .build();
//    }



    @GetMapping("/ready")
    public ApiResponse<List<RefundResponse>> getReadyRefunds(){

        return ApiResponse.<List<RefundResponse>>builder()
                .result(refundService.getReadyRefunds())
                .build();
    }




    @PostMapping("/{refundId}/refund-checkout")
    public ApiResponse<String> refundCheckout(
            @PathVariable String refundId,
            HttpServletRequest request){
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + "/optics";
        return ApiResponse.<String>builder()
                .result(refundService.initiateRefundPayment(refundId, baseUrl))
                .build();
    }

//    @PostMapping("/complete/{refundId}")
//    public ApiResponse<RefundResponse> completeRefund(
//            @PathVariable String refundId){
//        String managerId = SecurityContextHolder.getContext().getAuthentication().getName();
//        return ApiResponse.<RefundResponse>builder()
//                .result(refundService.completeRefund(refundId,managerId))
//                .build();
//    }
}
