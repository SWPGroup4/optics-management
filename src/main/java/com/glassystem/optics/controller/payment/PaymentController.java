package com.glassystem.optics.controller.payment;



import com.glassystem.optics.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/submitOrder")
    public String submitOrder(@RequestParam("amount") int orderTotal,
                              @RequestParam("orderInfo") String orderInfo,
                              HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl); //
        return "redirect:" + vnpayUrl;
    }

    @GetMapping("/vnpay-payment")
    @ResponseBody // Trả về text trực tiếp lên màn hình browser/swagger
    public String paymentCallback(HttpServletRequest request) {
        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");

        if (paymentStatus == 1) {
            return "THANH TOAN THANH CONG | Ma GD: " + transactionId + " | Noi dung: " + orderInfo;
        } else if (paymentStatus == 0) {
            return "THANH TOAN THAT BAI";
        } else {
            return "LOI XAC THUC CHU KY (Check HashSecret)";
        }
    }
}