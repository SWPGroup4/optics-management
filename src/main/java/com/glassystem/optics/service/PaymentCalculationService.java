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
                .map(item -> toItemPaymentRequirement(item))
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

        BigDecimal baseItemTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal lensPricePerUnit = item.getLensPrice() == null
                ? BigDecimal.ZERO
                : item.getLensPrice();
        BigDecimal lensPriceTotal = lensPricePerUnit
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal itemTotal = baseItemTotal.add(lensPriceTotal);

        BigDecimal paymentPercentage = getPaymentPercentage(item);

        BigDecimal requiredPayment = baseItemTotal.multiply(paymentPercentage)
                .add(lensPriceTotal);

        return ItemPaymentRequirement.builder()
                .orderItemId(item.getId())
                .orderItemType(item.getOrderItemType())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lensPrice(lensPricePerUnit)
                .lensPriceTotal(lensPriceTotal)
                .baseItemTotal(baseItemTotal)
                .itemTotal(itemTotal)
                .paymentPercentage(paymentPercentage)
                .requiredPayment(requiredPayment)
                .build();
    }

    private BigDecimal getPaymentPercentage(OrderItem item) {

        // Rule cho phần giá sản phẩm gốc
        return switch (item.getOrderItemType()) {
            case IN_STOCK -> BigDecimal.ONE;
            case PRE_ORDER -> new BigDecimal("0.5");
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

        BigDecimal lensPrice;

        BigDecimal lensPriceTotal;

        BigDecimal baseItemTotal;

        BigDecimal itemTotal;

        BigDecimal paymentPercentage;

        BigDecimal requiredPayment;
    }
}
