package com.glassystem.optics.service;


import com.glassystem.optics.dto.request.BankInfoRequest;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.entity.*;
import com.glassystem.optics.enums.*;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.ProductVariantMapper;
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
    final ProductVariantRepository productVariantRepository;
    final ProductVariantMapper productVariantMapper;


    @Transactional
    public ProductVariantResponse inactivateVariant(String variantId){
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        variant.setStatus(ProductVariantStatus.INACTIVE);
        return productVariantMapper.toResponse(productVariantRepository.save(variant));
    }


    public List<Orders> getAffectedOrdersByVariant(String variantId) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED
                        && order.getStatus() != OrderStatus.REFUNDED)
                .filter(order ->
                        order.getItems().stream()
                                .anyMatch(item -> item.getOrderItemType() == OrderItemType.PRE_ORDER
                                        && item.getProductVariant() != null
                                        && variantId.equals(item.getProductVariant().getId()))
                )
                .toList();
    }

    public List<RefundResponse> getAffectedOrders(String variantId){
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        if(variant.getStatus() != ProductVariantStatus.INACTIVE){
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_INACTIVE);
        }
        List<Orders> orders = getAffectedOrdersByVariant(variantId);
        return orders.stream()
                .map(refundMapper::toRefundResponseFromOrder)
                .toList();
    }

    @Transactional
    public void createRefundRequest(String orderId){
        createRefundRequests(List.of(orderId));
    }


    @Transactional
    public RefundResponse createRefundRequests(List<String> orderIds){

        RefundResponse response = null;
        for(String orderId : orderIds){
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if(refundRepository.existsByOrderId(orderId)){
                continue;
            }
            Refund refund = new Refund();
            refund.setOrder(order);
            refund.setStatus(RefundStatus.WAITING_CUSTOMER_INFO);
            refund.setCreatedAt(LocalDateTime.now());

            response = refundMapper.toRefundResponse(refundRepository.save(refund));
        }
        return response;
    }

    @Transactional
    public void createRefundByVariant(String variantId) {
        List<Orders> orders = getAffectedOrdersByVariant(variantId);
        createRefundRequests(
                orders.stream()
                        .map(Orders::getId)
                        .toList()
        );
    }

    @Transactional
    public void submitBankInfo(String refundId, BankInfoRequest request){

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));

        if(refund.getStatus() != RefundStatus.WAITING_CUSTOMER_INFO){
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
        }

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

        if (refund.getStatus() != RefundStatus.READY_FOR_REFUND) {
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
        }

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
