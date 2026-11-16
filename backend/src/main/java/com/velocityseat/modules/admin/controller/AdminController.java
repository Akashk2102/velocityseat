package com.velocityseat.modules.admin.controller;

import com.velocityseat.common.dto.ApiResponse;
import com.velocityseat.modules.admin.dto.AdminStatsResponse;
import com.velocityseat.modules.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Endpoints for platform management, inventory metrics, and revenue analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get system analytics and booking metrics (Admin only)",
               description = "Returns aggregated data on total venues, events, revenue, tickets sold, and active holds")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getSystemStatistics() {
        AdminStatsResponse stats = adminService.getSystemStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Platform statistics retrieved successfully"));
    }
}
