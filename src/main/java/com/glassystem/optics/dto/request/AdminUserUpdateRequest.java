package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserUpdateRequest extends UserUpdateRequest {
    List<String> roles;
    UserStatus status;
}
