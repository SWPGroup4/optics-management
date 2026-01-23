package com.glassystem.optics.dto.request;

import com.glassystem.optics.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserUpdateRequest {
    List<String> roles;
    UserStatus status;
}
