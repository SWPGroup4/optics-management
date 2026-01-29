package com.glassystem.optics.controller.access;

import java.util.List;

import com.glassystem.optics.dto.request.PermissionRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.PermissionResponse;
import com.glassystem.optics.service.PermissionService;

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
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Access Control - Permission Management", description = "Endpoints for managing granular system permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Create a new permission", description = "Define a new action-based permission in the system")
    ApiResponse<PermissionResponse> create(@RequestBody @Valid PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all permissions", description = "Retrieve a list of all defined permissions for role assignment")
    ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    @DeleteMapping("/{permissionName}")
    @Operation(summary = "Delete permission", description = "Permanently remove a permission from the system")
    ApiResponse<Void> delete(@PathVariable("permissionName") String permissionName) {
        permissionService.delete(permissionName);
        return ApiResponse.<Void>builder()
                .message("Permission deleted successfully")
                .build();
    }
}