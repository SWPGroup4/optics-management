package com.glassystem.optics.service;

import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.entity.Transaction;
import com.glassystem.optics.enums.*;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.repository.OrderRepository;
import com.glassystem.optics.repository.PaymentRepository;
import com.glassystem.optics.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentService {
    final OrderRepository orderRepository;
    final PaymentRepository paymentRepository;
    final TransactionRepository transactionRepository;
    final VNPayService vnPayService;


    @Transactional
    public String initiatePayment(String orderid, PaymentMethod paymentMethod, String baseUrl){
        Orders order = orderRepository.findById(orderid)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        if(order.getStatus().equals(OrderStatus.COMPLETED)){
            throw new AppException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        BigDecimal amountToPay = order.getDepositAmount();

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentPurpose(amountToPay.equals(order.getTotalAmount()) ? PaymentPurpose.FULL : PaymentPurpose.DEPOSIT)
                .amount(amountToPay)
                .status(PaymentStatus.UNPAID)
                .build();

        payment = paymentRepository.save(payment);

        if(paymentMethod.equals(PaymentMethod.VNPAY)){
            return  vnPayService.createPaymentUrl(payment, baseUrl);
        }

        return "/order-success";
    }

    @Transactional
    public Payment processVnPayCallback(HttpServletRequest request){
        int payment_status = vnPayService.orderReturn(request);


        String vnp_TxnRef = request.getParameter("vnp_TxnRef"); // Payment ID
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String vnp_Amount = request.getParameter("vnp_Amount");

        Payment payment = paymentRepository.findById(vnp_TxnRef)
                .orElseThrow(() -> new RuntimeException("Payment Not Found"));

        if(payment_status ==1 ){
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(LocalDateTime.now());


            Transaction transaction = Transaction.builder()
                    .payment(payment)
                    .type(TransactionType.CHARGE)
                    .amount(new BigDecimal(vnp_Amount).divide(new BigDecimal(100)))
                    .gatewayReference(vnp_TransactionNo)
                    .build();
                    transactionRepository.save(transaction);

                    Orders order = payment.getOrder();
                    updateOrderStatusBasedOnItems(order);
                    orderRepository.save(order);

        }else{
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDate(LocalDateTime.now());

        }
        return paymentRepository.save(payment);
    }

    private void updateOrderStatusBasedOnItems(Orders order){
        boolean hasSpecialItem = false;

        for(OrderItem orderItem : order.getItems()){
            if(orderItem.getOrderItemType().equals(OrderItemType.PRESCRIPTION) ||
                orderItem.getOrderItemType().equals(OrderItemType.PRE_ORDER)){
                hasSpecialItem = true;
                break;
            }
        }
        if(hasSpecialItem){
            order.setStatus(OrderStatus.PROCESSING);
        }else{
            order.setStatus(OrderStatus.COMPLETED);
        }
    }
}
