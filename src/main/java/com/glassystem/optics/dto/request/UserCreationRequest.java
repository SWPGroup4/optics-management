package com.glassystem.optics.dto.request;


import java.time.LocalDate;

import com.glassystem.optics.validatory.DobConstraint;
import com.glassystem.optics.validatory.Gmail;
import com.glassystem.optics.validatory.VietNamPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @Size(min = 3, message = "USERNAME_INVALID")
    String username;

    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;

    String firstName;
    String lastName;

    @DobConstraint(min = 10, message = "INVALID_DOB")
    LocalDate dob;
    @Schema(hidden = true)
    String imageUrl;
    @Gmail(message = "INVALID_GMAIL")
    String email;
    @VietNamPhone(message = "INVALID_VNPHONE")
    String phone;
}