package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.PermissionRequest;
import com.glassystem.optics.dto.response.PermissionResponse;
import com.glassystem.optics.entity.Permission;
import org.mapstruct.Mapper;



@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);

    PermissionResponse toPermissionResponse(Permission permission);
}