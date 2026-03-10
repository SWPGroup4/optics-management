package com.glassystem.optics.controller;

import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.RevenueDashboardResponse;
import com.glassystem.optics.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Dashboard", description = "Revenue dashboard and system statistics")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class DashboardController {

    DashboardService dashboardService;

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue dashboard",
            description = "Returns revenue, revenue growth %, active orders, orders today, pending orders, and low stock items count")
    public ApiResponse<RevenueDashboardResponse> getRevenueDashboard() {
        return ApiResponse.<RevenueDashboardResponse>builder()
                .result(dashboardService.getRevenueDashboard())
                .build();
    }
}
