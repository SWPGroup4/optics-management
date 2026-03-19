package com.glassystem.optics.controller.payment;

import com.glassystem.optics.dto.request.PaymentRequirementRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.PaymentRequirementResponse;
import com.glassystem.optics.dto.response.PaymentResponse;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentPurpose;
import com.glassystem.optics.enums.PaymentStatus;
import com.glassystem.optics.service.OrderService;
import com.glassystem.optics.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Controller")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentController {
    final PaymentService paymentService;
    final OrderService orderService;
    @Value("${app.frontend-url}")
    String frontendUrl;


    @PostMapping("/orders/requirement")
    public ApiResponse<PaymentRequirementResponse> getPaymentRequirement(
            @RequestBody @Valid PaymentRequirementRequest request) {
        return ApiResponse.<PaymentRequirementResponse>builder()
                .result(orderService.getPaymentRequirement(request))
                .build();
    }



    @PostMapping("/checkout")
    public ApiResponse<String> checkout(@RequestParam String orderId, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + "/optics";

        String paymentUrl = paymentService.initiatePayment(orderId, PaymentMethod.VNPAY, baseUrl);

        return ApiResponse.<String>builder()
                .result(paymentUrl)
                .build();
    }

    @GetMapping("/vnpay-callback")
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Payment payment = paymentService.processVnPayCallback(request);
        if (payment == null) {
            response.sendRedirect(String.format("%s/checkout/failure", frontendUrl));
            return;
        }
        if (payment.getPaymentPurpose() == PaymentPurpose.REFUND
                && payment.getStatus() == PaymentStatus.REFUNDED) {
            String orderId = payment.getOrder().getId();
            String email = payment.getOrder().getCustomer().getEmail();
            response.sendRedirect(String.format("%s/refund/success?orderId=%s&email=%s",
                    frontendUrl, orderId, email));
            return;
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            String orderId = payment.getOrder().getId();
            String email = payment.getOrder().getCustomer().getEmail();
            response.sendRedirect(String.format("%s/checkout/success?orderId=%s&email=%s",
                    frontendUrl, orderId, email));
            return;
        }
        response.sendRedirect(String.format("%s/checkout/failure", frontendUrl));
    }


    @GetMapping("/orders/{orderId}/history")
    public ApiResponse<List<PaymentResponse>> getPaymentHistory(
            @PathVariable String orderId) {
        return ApiResponse.<List<PaymentResponse>>builder()
                .result(paymentService.getPaymentHistory(orderId))
                .build();
    }

}
