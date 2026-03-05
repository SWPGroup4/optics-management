package com.glassystem.optics.controller.payment;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.PaymentRequirementResponse;
import com.glassystem.optics.dto.response.PaymentResponse;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentStatus;
import com.glassystem.optics.repository.PaymentRepository;
import com.glassystem.optics.service.OrderService;
import com.glassystem.optics.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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


    @GetMapping("/orders/{orderId}/requirement")
    public ApiResponse<PaymentRequirementResponse> getPaymentRequirement(
            @PathVariable String orderId) {
        return ApiResponse.<PaymentRequirementResponse>builder()
                .result(orderService.getPaymentRequirement(orderId))
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

    @PostMapping("/{paymentId}/refund")
    public ApiResponse<String> refundPayment(@PathVariable String paymentId, HttpServletRequest request) {
        paymentService.processRefund(paymentId, request);
        return ApiResponse.<String>builder()
                .result("Refund processed successfully")
                .build();
    }

    @GetMapping("/vnpay-callback")
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Payment payment = paymentService.processVnPayCallback(request);

        if (payment != null && payment.getStatus().equals((PaymentStatus.PAID))) {
            String orderId = payment.getOrder().getId();
            String email = payment.getOrder().getCustomer().getEmail();

            response.sendRedirect(String.format("%s/checkout/success?orderId=%s&email=%s",
                    frontendUrl, orderId, email));
        } else {
            response.sendRedirect(String.format("%s/checkout/failure", frontendUrl));

        }
    }

    @GetMapping("/orders/{orderId}/history")
    public ApiResponse<List<PaymentResponse>> getPaymentHistory(
            @PathVariable String orderId) {
        return ApiResponse.<List<PaymentResponse>>builder()
                .result(paymentService.getPaymentHistory(orderId))
                .build();
    }

}