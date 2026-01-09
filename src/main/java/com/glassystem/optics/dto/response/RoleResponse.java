package com.glassystem.optics.dto.response;


import java.util.Set;


import com.glassystem.optics.dto.request.PermissionRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    private String name;
    private String description;
    Set<PermissionRequest> permissions;
}
