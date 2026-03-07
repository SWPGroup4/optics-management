package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.OrderItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemCreationRequest {

    @NotBlank(message = "FIELD_MISSING")
    String productVariantId;



    @NotNull(message = "QUANTITY_INVALID")
    @Min(value = 1, message = "INVALID_QUANTITY")
    Integer quantity;
    PrescriptionRequest prescription;
}
