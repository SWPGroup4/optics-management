package com.glassystem.optics.service;

import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.enums.OrderItemType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentCalculationService {

    public PaymentCalculationResult calculatePaymentRequirement(List<OrderItem> items) {
        List<ItemPaymentRequirement> itemRequirements = items.stream()
                .map(this::toItemPaymentRequirement)
                .toList();

        BigDecimal orderTotal = itemRequirements.stream()
                .map(ItemPaymentRequirement::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requiredPaymentTotal = itemRequirements.stream()
                .map(ItemPaymentRequirement::getRequiredPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentCalculationResult.builder()
                .orderTotal(orderTotal)
                .requiredPaymentTotal(requiredPaymentTotal)
                .itemRequirements(itemRequirements)
                .build();
    }

    private ItemPaymentRequirement toItemPaymentRequirement(OrderItem item) {
        BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        double paymentPercentage = getPaymentPercentage(item.getOrderItemType());
        BigDecimal requiredPayment = itemTotal.multiply(BigDecimal.valueOf(paymentPercentage));

        return ItemPaymentRequirement.builder()
                .orderItemId(item.getId())
                .orderItemType(item.getOrderItemType())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .itemTotal(itemTotal)
                .paymentPercentage(paymentPercentage)
                .requiredPayment(requiredPayment)
                .build();
    }

    private double getPaymentPercentage(OrderItemType orderItemType) {
        return switch (orderItemType) {
            case IN_STOCK, PRESCRIPTION -> 1.0;
            case PRE_ORDER -> 0.5;
        };
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PaymentCalculationResult {
        BigDecimal orderTotal;
        BigDecimal requiredPaymentTotal;
        List<ItemPaymentRequirement> itemRequirements;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemPaymentRequirement {
        String orderItemId;
        OrderItemType orderItemType;
        Integer quantity;
        BigDecimal unitPrice;
        BigDecimal itemTotal;
        double paymentPercentage;
        BigDecimal requiredPayment;
    }
}
