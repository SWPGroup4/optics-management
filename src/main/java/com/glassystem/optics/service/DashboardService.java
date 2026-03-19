package com.glassystem.optics.service;

import com.glassystem.optics.dto.response.RevenueDashboardResponse;
import com.glassystem.optics.enums.OrderStatus;
import com.glassystem.optics.enums.PaymentStatus;
import com.glassystem.optics.repository.InventoryRepository;
import com.glassystem.optics.repository.OrderRepository;
import com.glassystem.optics.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DashboardService {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    InventoryRepository inventoryRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Transactional(readOnly = true)
    public RevenueDashboardResponse getRevenueDashboard() {
        BigDecimal revenue = getTotalRevenue();
        Double revenueGrowth = getRevenueGrowth();
        long activeOrders = getActiveOrdersCount();
        long ordersToday = getOrdersTodayCount();
        long returnPending = getReturnPendingCount();
        long lowStockItems = getLowStockItemsCount();

        return RevenueDashboardResponse.builder()
                .revenue(revenue)
                .revenueGrowth(revenueGrowth)
                .activeOrders(activeOrders)
                .ordersToday(ordersToday)
                .returnPending(returnPending)
                .lowStockItems(lowStockItems)
                .build();
    }

    private BigDecimal getTotalRevenue() {
        return paymentRepository.sumAmountByStatus(PaymentStatus.PAID);
    }

    private Double getRevenueGrowth() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        LocalDateTime previousMonthStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousMonthEnd = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal currentMonthRevenue = paymentRepository.sumAmountByStatusAndPaymentDateBetween(
                PaymentStatus.PAID, currentMonthStart, currentMonthEnd);

        BigDecimal previousMonthRevenue = paymentRepository.sumAmountByStatusAndPaymentDateBetween(
                PaymentStatus.PAID, previousMonthStart, previousMonthEnd);

        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        return currentMonthRevenue.subtract(previousMonthRevenue)
                .divide(previousMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long getActiveOrdersCount() {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.AWAITING_VERIFICATION,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.PROCESSING,
                OrderStatus.PRODUCED,
                OrderStatus.SHIPPED
        );
        return orderRepository.countByStatusIn(activeStatuses);
    }

    private long getOrdersTodayCount() {
        return orderRepository.countByCreatedAt(LocalDate.now());
    }

    private long getReturnPendingCount() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }

    private long getLowStockItemsCount() {
        return inventoryRepository.countLowStockItems(LOW_STOCK_THRESHOLD);
    }
}