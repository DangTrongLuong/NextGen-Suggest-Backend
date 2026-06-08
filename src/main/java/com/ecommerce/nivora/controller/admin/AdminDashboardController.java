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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/dashboard", "/api/admin/dashboard"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Admin analytics dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard statistics (compat)")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardCompat() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard overall statistics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }

    @GetMapping("/revenue-chart")
    @Operation(summary = "Get dashboard daily revenue chart data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueChart() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRevenueChart()));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get dashboard top selling products chart data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTopProducts()));
    }

    @GetMapping("/category-stats")
    @Operation(summary = "Get dashboard product category stats chart data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCategoryStats()));
    }
}
