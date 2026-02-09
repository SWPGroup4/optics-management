package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.PaymentMethod;
import com.glassystem.optics.validatory.VietNamPhone;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class OrderUpdateRequest {
    @NotBlank(message = "FIELD_MISSING")
    String deliveryAddress;

    @NotBlank(message = "FIELD_MISSING")
    @VietNamPhone(message = "INVALID_VNPHONE")
    String phoneNumber;

    @Valid
    @NotEmpty(message = "LIST_EMPTY")
    List<OrderItemUpdateRequest> items;
}
