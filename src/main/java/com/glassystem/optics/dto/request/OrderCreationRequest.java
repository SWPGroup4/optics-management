package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.validatory.VietNamPhone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderCreationRequest {

    @NotBlank(message = "FIELD_MISSING")
    String deliveryAddress;
    @NotBlank(message = "FIELD_MISSING")
    @VietNamPhone(message = "INVALID_VNPHONE")
    String phoneNumber;
    PaymentMethod paymentMethod;
    @Valid
            @NotEmpty(message = "LIST_EMPTY")
    List<OrderItemCreationRequest> items;
}
