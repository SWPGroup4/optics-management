package com.glassystem.optics.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LensCreateRequest {

    @NotBlank(message = "FIELD_MISSING")
    String name;

    String material;

    @NotNull(message = "INVALID_PRICE")
    @DecimalMin(value = "0.0", inclusive = true, message = "INVALID_PRICE")
    BigDecimal price;

    String description;
}
