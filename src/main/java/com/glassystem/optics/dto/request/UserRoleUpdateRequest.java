package com.glassystem.optics.dto.request;


import com.glassystem.optics.enums.UserRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRoleUpdateRequest {
    Set<UserRole> roles;
}
