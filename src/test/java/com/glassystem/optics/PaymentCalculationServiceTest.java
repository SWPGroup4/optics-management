package com.glassystem.optics;

import com.glassystem.optics.entity.OrderItem;
import com.glassystem.optics.enums.OrderItemType;
import com.glassystem.optics.service.PaymentCalculationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentCalculationServiceTest {

    private final PaymentCalculationService paymentCalculationService = new PaymentCalculationService();

    @Test
    void calculatePaymentRequirement_shouldCalculatePerItemForMixedTypes() {
        OrderItem inStock = OrderItem.builder()
                .orderItemType(OrderItemType.IN_STOCK)
                .unitPrice(BigDecimal.valueOf(100))
                .quantity(1)
                .build();

        OrderItem prescription = OrderItem.builder()
                .orderItemType(OrderItemType.PRESCRIPTION)
                .unitPrice(BigDecimal.valueOf(200))
                .quantity(1)
                .build();

        OrderItem preOrder = OrderItem.builder()
                .orderItemType(OrderItemType.PRE_ORDER)
                .unitPrice(BigDecimal.valueOf(300))
                .quantity(1)
                .build();

        PaymentCalculationService.PaymentCalculationResult result =
                paymentCalculationService.calculatePaymentRequirement(List.of(inStock, prescription, preOrder));

        assertEquals(BigDecimal.valueOf(600), result.getOrderTotal());
        assertEquals(BigDecimal.valueOf(450.0), result.getRequiredPaymentTotal());

        assertEquals(BigDecimal.valueOf(100.0), result.getItemRequirements().get(0).getRequiredPayment());
        assertEquals(BigDecimal.valueOf(200.0), result.getItemRequirements().get(1).getRequiredPayment());
        assertEquals(BigDecimal.valueOf(150.0), result.getItemRequirements().get(2).getRequiredPayment());
    }

    @Test
    void calculatePaymentRequirement_shouldSupportQuantityGreaterThanOne() {
        OrderItem preOrder = OrderItem.builder()
                .orderItemType(OrderItemType.PRE_ORDER)
                .unitPrice(BigDecimal.valueOf(300))
                .quantity(2)
                .build();

        PaymentCalculationService.PaymentCalculationResult result =
                paymentCalculationService.calculatePaymentRequirement(List.of(preOrder));

        assertEquals(BigDecimal.valueOf(600), result.getOrderTotal());
        assertEquals(BigDecimal.valueOf(300.0), result.getRequiredPaymentTotal());
        assertEquals(BigDecimal.valueOf(600), result.getItemRequirements().getFirst().getItemTotal());
        assertEquals(BigDecimal.valueOf(300.0), result.getItemRequirements().getFirst().getRequiredPayment());
    }
}
