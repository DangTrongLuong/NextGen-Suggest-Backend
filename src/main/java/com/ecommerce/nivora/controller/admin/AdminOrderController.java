package com.ecommerce.nivora.controller.admin;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.OrderResponse;
import com.ecommerce.nivora.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Admin order management")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(status, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByIdAdmin(id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateStatus(id, status)));
    }
}
