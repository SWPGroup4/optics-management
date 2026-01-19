package com.glassystem.optics.dto.request;


import java.time.LocalDate;
import java.util.List;


import com.glassystem.optics.validatory.DobConstraint;
import com.glassystem.optics.validatory.Gmail;
import com.glassystem.optics.validatory.VietNamPhone;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;
    String firstName;
    String lastName;

    @DobConstraint(min = 9, message = "INVALID_DOB")
    LocalDate dob;

    String imageUrl;
    @Gmail(message = "INVALID_GMAIL")
    String email;
    @VietNamPhone(message = "INVALID_VNPHONE")
    String phone;

}
