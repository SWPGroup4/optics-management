package com.glassystem.optics.service;


import com.glassystem.optics.dto.request.BankInfoRequest;
import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.*;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.RefundMapper;
import com.glassystem.optics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundService {

    final RefundRepository refundRepository;
    final OrderRepository orderRepository;
    final PaymentRepository paymentRepository;
    final RefundMapper  refundMapper;

    @Transactional
    public void createRefundRequest(String orderId){

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Refund refund = Refund.builder()
                .order(order)
                .customerId(order.getCustomer().getId())
                .refundAmount(order.getDepositAmount())
                .status(RefundStatus.WAITING_CUSTOMER_INFO)
                .createdAt(LocalDateTime.now())
                .build();

        refundRepository.save(refund);
    }

    @Transactional
    public void submitBankInfo(String refundId, BankInfoRequest request){

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));

        refund.setBankName(request.getBankName());
        refund.setBankAccountNumber(request.getBankAccountNumber());
        refund.setAccountHolderName(request.getAccountHolderName());

        refund.setStatus(RefundStatus.READY_FOR_REFUND);

        refundRepository.save(refund);
    }

    public List<RefundResponse> getReadyRefunds(){

        return refundRepository.findByStatus(RefundStatus.READY_FOR_REFUND)
                .stream()
                .map(refundMapper::toRefundResponse)
                .toList();
    }

    @Transactional
    public void completeRefund(String refundId, String managerId){

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));

        Orders order = refund.getOrder();

        Payment payment = paymentRepository.findFirstByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setProcessedBy(managerId);

        order.setStatus(OrderStatus.REFUNDED);

        payment.setStatus(PaymentStatus.REFUNDED);

        refundRepository.save(refund);
        orderRepository.save(order);
        paymentRepository.save(payment);
    }
}
