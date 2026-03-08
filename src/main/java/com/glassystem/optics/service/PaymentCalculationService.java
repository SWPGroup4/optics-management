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

        return buildResult(itemRequirements);
    }

    public PaymentCalculationResult calculatePaymentRequirementForPreview(List<PaymentItemInput> items) {
        List<ItemPaymentRequirement> itemRequirements = items.stream()
                .map(this::toItemPaymentRequirement)
                .toList();

        return buildResult(itemRequirements);
    }

    private PaymentCalculationResult buildResult(List<ItemPaymentRequirement> itemRequirements) {
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
        return buildItemRequirement(
                item.getId(),
                item.getOrderItemType(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLensPrice()
        );
    }

    private ItemPaymentRequirement toItemPaymentRequirement(PaymentItemInput item) {
        return buildItemRequirement(
                item.getOrderItemId(),
                item.getOrderItemType(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLensPrice()
        );
    }

    private ItemPaymentRequirement buildItemRequirement(
            String orderItemId,
            OrderItemType orderItemType,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lensPrice
    ) {
        BigDecimal safeUnitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal safeLensPrice = lensPrice == null ? BigDecimal.ZERO : lensPrice;
        BigDecimal safeQuantity = BigDecimal.valueOf(quantity == null ? 0 : quantity);

        BigDecimal baseItemTotal = safeUnitPrice.multiply(safeQuantity);
        BigDecimal lensPriceTotal = safeLensPrice.multiply(safeQuantity);
        BigDecimal itemTotal = baseItemTotal.add(lensPriceTotal);

        BigDecimal paymentPercentage = getPaymentPercentage(orderItemType);
        BigDecimal requiredPayment = baseItemTotal.multiply(paymentPercentage).add(lensPriceTotal);

        return ItemPaymentRequirement.builder()
                .orderItemId(orderItemId)
                .orderItemType(orderItemType)
                .quantity(quantity)
                .unitPrice(safeUnitPrice)
                .lensPrice(safeLensPrice)
                .lensPriceTotal(lensPriceTotal)
                .baseItemTotal(baseItemTotal)
                .itemTotal(itemTotal)
                .paymentPercentage(paymentPercentage)
                .requiredPayment(requiredPayment)
                .build();
    }

    private BigDecimal getPaymentPercentage(OrderItemType orderItemType) {
        return switch (orderItemType) {
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
    public static class PaymentItemInput {
        String orderItemId;
        OrderItemType orderItemType;
        Integer quantity;
        BigDecimal unitPrice;
        BigDecimal lensPrice;
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
