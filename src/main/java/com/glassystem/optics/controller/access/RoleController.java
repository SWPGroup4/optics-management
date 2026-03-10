package com.glassystem.optics.controller.access;

import java.util.List;

import com.glassystem.optics.dto.request.RoleRequest;
import com.glassystem.optics.dto.request.UserRoleUpdateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.RoleResponse;
import com.glassystem.optics.dto.response.UserResponse;
import com.glassystem.optics.enums.UserRole;
import com.glassystem.optics.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Access Control - Role Management")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    RoleService roleService;

    @PostMapping
    @Operation(summary = "Create a new role with permissions")
    ApiResponse<RoleResponse> create(@RequestBody @Valid RoleRequest roleRequest) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.create(roleRequest))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all roles in the system")
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{roleName}")
    @Operation(summary = "Delete a role by name")
    ApiResponse<Void> delete(@PathVariable("roleName") String roleName) {
        roleService.delete(roleName);
        return ApiResponse.<Void>builder()
                .message("Role deleted successfully")
                .build();
    }


    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upgrade or change roles for a specific user")
    ApiResponse<UserResponse> changeRole(
            @PathVariable String userId,
            @RequestParam("newRole") UserRole newRole) {
        return ApiResponse.<UserResponse>builder()
                .result(roleService.changeUserRole(userId, newRole))
                .build();
    }
}