package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.PaymentMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderUpdateRequest {
    String deliveryAddress;
    PaymentMethod paymentMethod;
    List<OrderItemUpdateRequest> items;
}
