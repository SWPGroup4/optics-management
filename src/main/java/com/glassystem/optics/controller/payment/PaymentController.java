package com.glassystem.optics.controller.payment;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.enums.PaymentStatus;
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

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Controller")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentController {
    final PaymentService paymentService;
    @Value("${app.frontend-url}")
    String frontendUrl;

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

        if (payment != null && payment.getStatus().equals(String.valueOf(PaymentStatus.PAID))) {
            String orderId = payment.getOrder().getId();
            String email = payment.getOrder().getCustomer().getEmail();

            response.sendRedirect(String.format("%s/checkout/success?orderId=%s&email=%s",
                    frontendUrl, orderId, email));
        } else {
            response.sendRedirect(String.format(frontendUrl + "%s/checkout/failure"));
        }

    }

}