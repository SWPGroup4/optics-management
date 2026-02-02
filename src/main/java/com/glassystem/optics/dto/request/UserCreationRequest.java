package com.glassystem.optics.dto.request;


import java.time.LocalDate;

import com.glassystem.optics.validatory.DobConstraint;
import com.glassystem.optics.validatory.Gmail;
import com.glassystem.optics.validatory.VietNamPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotBlank(message = "FIELD_MISSING")
    @Size(min = 3, message = "USERNAME_INVALID")
    @Column(unique = true, nullable = false)
    String username;

    @NotBlank(message = "FIELD_MISSING")
    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;

    @NotBlank(message = "FIELD_MISSING")
    @Gmail(message = "INVALID_GMAIL")
    @Column(unique = true, nullable = false)

    String email;

    @Schema(hidden = true)
    String imageUrl;


}