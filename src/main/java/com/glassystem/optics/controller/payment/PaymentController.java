package com.glassystem.optics.controller.payment;



import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.service.PaymentService;
import com.glassystem.optics.service.VNPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Controller")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {
    final PaymentService paymentService;

    @PostMapping("/checkout")
    public ApiResponse<String> checkout(@RequestParam String orderId,
                                        @RequestParam PaymentMethod paymentMethod,
                                        HttpServletRequest request){
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/optics";
        PaymentMethod method = PaymentMethod.valueOf(paymentMethod.name());

        String paymentUrl = paymentService.initiatePayment(orderId, method, baseUrl);

        return ApiResponse.<String>builder()
                .result(paymentUrl)
                .build();
    }


    @GetMapping("/vnpay-callback")
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String status = paymentService.processVnPayCallback(request);

        if ("SUCCESS".equals(status)) {
            // Redirect về trang Frontend báo thành công
            response.sendRedirect("https://optics-management-frontend.vercel.app/checkout/" +
                    "success?orderId=ORD-2026-7452&email=customer%40example.com");
        } else {
            // Redirect về trang Frontend báo lỗi
            response.sendRedirect("http://localhost:3000/payment/failed");
        }
    }



}