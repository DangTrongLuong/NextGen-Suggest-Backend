package com.ecommerce.nivora.controller.admin;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.DashboardResponse;
import com.ecommerce.nivora.service.DashboardService;
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
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Admin analytics dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }
}
