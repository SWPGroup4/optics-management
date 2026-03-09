package com.glassystem.optics.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueDashboardResponse {
    BigDecimal revenue;
    Double revenueGrowth;
    Long activeOrders;
    Long ordersToday;
    Long returnPending;
    Long lowStockItems;
}
