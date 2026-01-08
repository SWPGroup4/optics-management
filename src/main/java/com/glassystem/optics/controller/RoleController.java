package com.glassystem.optics.controller;


import java.util.List;

import com.glassystem.optics.dto.request.RoleRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.RoleResponse;
import com.glassystem.optics.service.RoleService;
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
public class RoleController {
    RoleService roleService;

    @PostMapping
    ApiResponse<RoleResponse> create(@RequestBody RoleRequest roleRequest) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.create(roleRequest))
                .build();
    }

    @GetMapping
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{role}")
    ApiResponse<Void> delete(@PathVariable String role) {
        roleService.delete(role);
        return ApiResponse.<Void>builder()
                .build();
    }
}
