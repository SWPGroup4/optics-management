package com.glassystem.optics.service;


import com.glassystem.optics.dto.request.BankInfoRequest;
import com.glassystem.optics.dto.response.OrderResponse;
import com.glassystem.optics.dto.response.ProductVariantResponse;
import com.glassystem.optics.dto.response.RefundBankAccountResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundService {

    static final BigDecimal CUSTOMER_CANCEL_REFUND_PERCENT = new BigDecimal("95");
    static final BigDecimal MANUFACTURER_CANCEL_REFUND_PERCENT = new BigDecimal("100");

    final RefundRepository refundRepository;
    final OrderRepository orderRepository;
    final PaymentRepository paymentRepository;
    final RefundMapper  refundMapper;
    final ProductVariantRepository productVariantRepository;
    final ProductVariantMapper productVariantMapper;
    final VNPayService vnPayService;
    final OrderService orderService;




    @Transactional
    public String initiateRefundPayment(String refundId, String baseUrl){
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));

        if(refund.getStatus() != RefundStatus.READY_FOR_REFUND
                && refund.getStatus() != RefundStatus.FAILED){
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
        }

        BigDecimal amount = refund.getRefundAmount();

        if (amount == null || amount.compareTo(new BigDecimal("5000")) < 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        populateRefundBankInfo(refund);

        if (refund.getBankName() == null || refund.getBankName().isBlank()
                || refund.getBankAccountNumber() == null || refund.getBankAccountNumber().isBlank()
                || refund.getAccountHolderName() == null || refund.getAccountHolderName().isBlank()) {
            throw new AppException(ErrorCode.FIELD_MISSING);
        }


        Payment payment = Payment.builder()
                .order(refund.getOrder())
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentPurpose(PaymentPurpose.REFUND)
                .amount(amount)
                .status(PaymentStatus.UNPAID)
                .description("Refund payment" + refund.getOrder().getId())
                .build();

        payment = paymentRepository.save(payment);
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.PROCESSING);
        refundRepository.save(refund);
        return vnPayService.createPaymentUrl(payment, baseUrl);
    }






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
                .filter(order -> order.getPreOrderStatus() == PreOrderStatus.DEPOSIT_PAID)
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
        return getAffectedOrdersByVariant(variantId).stream()
                .map(order -> toRefundResponse(order, variantId))
                .filter(response -> response.getRefundAmount() != null
                        && response.getRefundAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }


    private RefundResponse toRefundResponse(Orders order, String variantId) {
        BigDecimal refundAmount = order.getItems().stream()
                .filter(item -> item.getOrderItemType() == OrderItemType.PRE_ORDER)
                .filter(item -> item.getProductVariant() != null)
                .filter(item -> variantId.equals(item.getProductVariant().getId()))
                .map(this::calculateRefundAmountForItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RefundResponse.builder()
                .order(orderService.toOrderResponse(order))
                .refundAmount(refundAmount)
                .refundPercentage(MANUFACTURER_CANCEL_REFUND_PERCENT)
                .deductionAmount(BigDecimal.ZERO)
                .build();
    }


    private BigDecimal calculateRefundAmountForItem(OrderItem item) {
        if (item.getDepositPrice() == null) {
            return BigDecimal.ZERO;
        }
        return item.getDepositPrice();
    }

    private void populateRefundBankInfo(Refund refund) {
        if (refund == null || refund.getOrder() == null) {
            return;
        }

        Orders order = refund.getOrder();
        if (refund.getBankName() == null || refund.getBankName().isBlank()) {
            refund.setBankName(order.getBankName());
        }
        if (refund.getBankAccountNumber() == null || refund.getBankAccountNumber().isBlank()) {
            refund.setBankAccountNumber(order.getBankAccountNumber());
        }
        if (refund.getAccountHolderName() == null || refund.getAccountHolderName().isBlank()) {
            refund.setAccountHolderName(order.getAccountHolderName());
        }
    }



    @Transactional
    public List<RefundResponse> createRefundRequests(List<String> orderIds){
        return orderIds.stream()
                .map(this::createSingleRefundRequest)
                .filter(refund -> refund != null)
                //.map(refundMapper::toRefundResponse)
                .toList();
    }


    private RefundResponse createSingleRefundRequest(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if(refundRepository.existsByOrderId(orderId)){
            return null;
        }

        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setCustomerId(order.getCustomer() != null ? order.getCustomer().getId() : null);
        refund.setOrderTotalAmount(order.getTotalAmount());
        populateRefundBankInfo(refund);

        if(order.getStatus() == OrderStatus.CANCELLED) {
            if (!applyCustomerCancellationRefund(refund, orderId)) {
                return null;
            }
        }else if(order.getPreOrderStatus() == PreOrderStatus.DEPOSIT_PAID){
            BigDecimal depositAmount = order.getDepositAmount() == null ? BigDecimal.ZERO : order.getDepositAmount();
            if(depositAmount.compareTo(BigDecimal.ZERO) <= 0){
                return null;
            }
            refund.setRefundPercentage(MANUFACTURER_CANCEL_REFUND_PERCENT);
            refund.setRefundAmount(resolveRefundAmount(order, depositAmount, MANUFACTURER_CANCEL_REFUND_PERCENT));
            refund.setDeductionAmount(resolveDeductionAmount(order, depositAmount, MANUFACTURER_CANCEL_REFUND_PERCENT));
        }else {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        refund.setStatus(RefundStatus.READY_FOR_REFUND);
        refund.setCreatedAt(LocalDateTime.now());

        Refund savedRefund = refundRepository.save(refund);
        sendRefundNotification(savedRefund, NotificationTemplate.REFUND_CREATED);
        return buildRefundResponse(savedRefund);
    }

    private boolean applyCustomerCancellationRefund(Refund refund, String orderId) {
        BigDecimal paidAmount = getPaidAmount(orderId);
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Orders order = refund.getOrder();
        refund.setRefundPercentage(CUSTOMER_CANCEL_REFUND_PERCENT);
        refund.setRefundAmount(resolveRefundAmount(order, paidAmount, CUSTOMER_CANCEL_REFUND_PERCENT));
        refund.setDeductionAmount(resolveDeductionAmount(order, paidAmount, CUSTOMER_CANCEL_REFUND_PERCENT));
        return true;
    }



    private BigDecimal getPaidAmount(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .filter(amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveRefundAmount(Orders order, BigDecimal paidAmount, BigDecimal refundPercent) {
        BigDecimal safePaidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal safeRefundPercent = refundPercent == null ? BigDecimal.ZERO : refundPercent;

        BigDecimal orderTotal = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal eligibleAmount = orderTotal.compareTo(BigDecimal.ZERO) <= 0
                ? safePaidAmount
                : safePaidAmount.min(orderTotal);

        BigDecimal refundAmount = eligibleAmount
                .multiply(safeRefundPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return refundAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : refundAmount;
    }

    private BigDecimal resolveDeductionAmount(Orders order, BigDecimal paidAmount, BigDecimal refundPercent) {
        BigDecimal safePaidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal safeRefundPercent = refundPercent == null ? BigDecimal.ZERO : refundPercent;

        BigDecimal orderTotal = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        BigDecimal eligibleAmount = orderTotal.compareTo(BigDecimal.ZERO) <= 0
                ? safePaidAmount
                : safePaidAmount.min(orderTotal);

        BigDecimal refundAmount = eligibleAmount
                .multiply(safeRefundPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal deductionAmount = eligibleAmount.subtract(refundAmount);

        return deductionAmount.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : deductionAmount;
    }

//    @Transactional
//    public RefundBankAccountResponse submitBankInfo(String refundId, BankInfoRequest request){
//
//        Refund refund = refundRepository.findById(refundId)
//                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
//
//        if(refund.getStatus() != RefundStatus.WAITING_CUSTOMER_INFO){
//            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
//        }
//
//        refund.setBankName(request.getBankName());
//        refund.setBankAccountNumber(request.getBankAccountNumber());
//        refund.setAccountHolderName(request.getAccountHolderName());
//
//        refund.setStatus(RefundStatus.READY_FOR_REFUND);
//
//        return refundMapper.toRefundBankAccountResponse(refundRepository.save(refund));
//    }

    public List<RefundResponse> getReadyRefunds(){

        return refundRepository.findByStatus(RefundStatus.READY_FOR_REFUND)
                .stream()
                .map(this::buildRefundResponse)
                .toList();
    }

//    @Transactional
//    public RefundResponse completeRefund(String refundId, String managerId){
//        Refund refund = refundRepository.findById(refundId)
//                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
//        if (refund.getStatus() != RefundStatus.READY_FOR_REFUND) {
//            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
//        }
//        return processRefundCompletion(refund, managerId);
//    }



    @Transactional
    public void completeRefundByPayment(Payment payment){
        Refund refund = refundRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getStatus() != RefundStatus.PROCESSING) {
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS);
        }
        String processBy  = SecurityContextHolder.getContext().getAuthentication().getName();

        processRefundCompletion(refund, processBy);
    }

    @Transactional
    public void markRefundPaymentFailed(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return;
        }

        refundRepository.findByPaymentId(payment.getId())
                .ifPresent(refund -> {
                    if (refund.getStatus() == RefundStatus.PROCESSING) {
                        refund.setStatus(RefundStatus.FAILED);
                        refund.setPayment(null);
                        Refund savedRefund = refundRepository.save(refund);
                        sendRefundNotification(savedRefund, NotificationTemplate.REFUND_FAILED);
                    }
                });
    }

    private RefundResponse processRefundCompletion(Refund refund, String processedBy){
        Orders order = refund.getOrder();
        List<Payment> paidPayments = paymentRepository.findByOrderId(order.getId())
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .toList();
        if (paidPayments.isEmpty()) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (refund.getOrderTotalAmount() == null) {
            refund.setOrderTotalAmount(order.getTotalAmount());
        }
        if (refund.getCustomerId() == null && order.getCustomer() != null) {
            refund.setCustomerId(order.getCustomer().getId());
        }
        if (refund.getRefundAmount() == null) {
            BigDecimal basePaidAmount = order.getStatus() == OrderStatus.CANCELLED
                    ? getPaidAmount(order.getId())
                    : (order.getDepositAmount() == null ? BigDecimal.ZERO : order.getDepositAmount());

            refund.setRefundAmount(basePaidAmount);
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setProcessedBy(processedBy);

        order.setStatus(OrderStatus.REFUNDED);

        paidPayments.forEach(payment -> payment.setStatus(PaymentStatus.REFUNDED));

        Refund savedRefund = refundRepository.save(refund);
        orderRepository.save(order);
        paymentRepository.saveAll(paidPayments);
        sendRefundNotification(savedRefund, NotificationTemplate.REFUND_COMPLETED);

        return buildRefundResponse(savedRefund);
    }

    private RefundResponse buildRefundResponse(Refund refund) {
        RefundResponse response = refundMapper.toRefundResponse(refund);
        if (response == null) {
            return null;
        }

        Orders order = refund.getOrder();
        if (order != null) {
            response.setOrder(orderService.toOrderResponse(order));
        }

        return response;
    }

    private void sendRefundNotification(Refund refund, NotificationTemplate template) {
        if (refund == null
                || template == null
                || refund.getOrder() == null
                || refund.getOrder().getCustomer() == null
                || refund.getOrder().getCustomer().getId() == null) {
            return;
        }

        notificationService.createSystemNotification(
                refund.getOrder().getCustomer().getId(),
                template,
                refund.getOrder().getId()
        );
    }
}
