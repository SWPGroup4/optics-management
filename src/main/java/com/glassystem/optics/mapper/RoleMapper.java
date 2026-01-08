package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.RoleRequest;
import com.glassystem.optics.dto.response.RoleResponse;
import com.glassystem.optics.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest roleRequest);

    RoleResponse toRoleResponse(Role role);
}
