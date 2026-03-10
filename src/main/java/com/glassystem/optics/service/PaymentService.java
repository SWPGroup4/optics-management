package com.glassystem.optics.service;

import com.glassystem.optics.dto.response.PaymentResponse;
import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.entity.Transaction;
import com.glassystem.optics.enums.*;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.PaymentMapper;
import com.glassystem.optics.repository.OrderRepository;
import com.glassystem.optics.repository.PaymentRepository;
import com.glassystem.optics.repository.TransactionRepository;
import com.glassystem.optics.util.VnPayDateUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentService {
    final OrderRepository orderRepository;
    final PaymentRepository paymentRepository;
    final TransactionRepository transactionRepository;
    final VNPayService vnPayService;
    final PaymentMapper paymentMapper;

    @Transactional
    public String initiatePayment(String orderId, PaymentMethod paymentMethod, String baseUrl) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus().equals(OrderStatus.COMPLETED)) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        PaymentPurpose purpose = determinePaymentPurpose(order);

        if (purpose == PaymentPurpose.REMAINING &&
                order.getStatus() != OrderStatus.AWAITING_FINAL_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        BigDecimal amount = getAmountToPay(order, purpose);

        if (amount.compareTo(BigDecimal.ZERO) > 0 && !paymentMethod.equals(PaymentMethod.VNPAY)) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentPurpose(purpose)
                .amount(amount)
                .status(PaymentStatus.UNPAID)
                .description("Payment for " + purpose.toString().toLowerCase())
                .build();

        payment = paymentRepository.save(payment);

        if (paymentMethod.equals(PaymentMethod.VNPAY)) {
            return vnPayService.createPaymentUrl(payment, baseUrl);
        }

        return "/order-confirmed";
    }

    @Transactional
    public Payment processVnPayCallback(HttpServletRequest request) {
        int payment_status = vnPayService.orderReturn(request);

        String vnp_TxnRef = request.getParameter("vnp_TxnRef"); // Payment ID
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String vnp_Amount = request.getParameter("vnp_Amount");
        String vnp_PayDate = request.getParameter("vnp_PayDate");

        Payment payment = paymentRepository.findById(vnp_TxnRef)
                .orElseThrow(() -> new RuntimeException("Payment Not Found"));

        if (payment_status == 1) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(LocalDateTime.now());

            TransactionType txnType = payment.getPaymentPurpose() == PaymentPurpose.DEPOSIT
                    ? TransactionType.DEPOSIT
                    : TransactionType.CHARGE;

            LocalDateTime payDate = VnPayDateUtil.parse(vnp_PayDate);
            Transaction transaction = Transaction.builder()
                    .payment(payment)
                    .type(txnType)
                    .amount(new BigDecimal(vnp_Amount).divide(new BigDecimal(100)))
                    .gatewayReference(vnp_TransactionNo)
                    .dateTime(payDate)
                    .build();
            transactionRepository.save(transaction);

            Orders order = payment.getOrder();

            if (payment.getPaymentPurpose() == PaymentPurpose.DEPOSIT) {
                order.setPreOrderStatus(PreOrderStatus.DEPOSIT_PAID);
            } else if (payment.getPaymentPurpose() == PaymentPurpose.REMAINING) {
                order.setPreOrderStatus(PreOrderStatus.REMAINING_PAID);
            }

            updateOrderStatusAfterPayment(order, payment);
            orderRepository.save(order);

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDate(LocalDateTime.now());

        }
        return paymentRepository.save(payment);
    }




    @Transactional
    public List<PaymentResponse> getPaymentHistory(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<Payment> payments = paymentRepository.findByOrderId(order.getId());

        return payments.stream()
                .sorted((p1, p2) -> {
                    // Handle null paymentDate
                    if (p1.getPaymentDate() == null && p2.getPaymentDate() == null)
                        return 0;
                    if (p1.getPaymentDate() == null)
                        return 1;
                    if (p2.getPaymentDate() == null)
                        return -1;
                    return p2.getPaymentDate().compareTo(p1.getPaymentDate()); // Desc order
                })
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    private void updateOrderStatusAfterPayment(Orders order, Payment payment) {
        if (payment.getPaymentPurpose() == PaymentPurpose.DEPOSIT) {
            order.setPreOrderStatus(PreOrderStatus.DEPOSIT_PAID);
            order.setStatus(OrderStatus.AWAITING_VERIFICATION);
        }
        if (payment.getPaymentPurpose() == PaymentPurpose.REMAINING) {
            order.setPreOrderStatus(PreOrderStatus.REMAINING_PAID);
            if (order.getStatus() == OrderStatus.AWAITING_FINAL_PAYMENT) {
                order.setStatus(OrderStatus.PREPARING);
            }
        }
        if (payment.getPaymentPurpose() == PaymentPurpose.FULL) {
            order.setStatus(OrderStatus.PREPARING);
        }
    }




    private BigDecimal getAmountToPay(Orders order, PaymentPurpose purpose) {
        return switch (purpose) {
            case DEPOSIT -> order.getDepositAmount();
            case REMAINING -> order.getRemainingAmount();
            case FULL -> order.getTotalAmount();
        };
    }

    private PaymentPurpose determinePaymentPurpose(Orders order) {
        if (order.getRemainingAmount() == null || order.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentPurpose.FULL;
        }

        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        boolean hasDepositPaid = payments.stream()
                .anyMatch(p -> p.getPaymentPurpose().equals(PaymentPurpose.DEPOSIT)
                        && p.getStatus().equals(PaymentStatus.PAID));
        return hasDepositPaid ? PaymentPurpose.REMAINING : PaymentPurpose.DEPOSIT;
    }

}
