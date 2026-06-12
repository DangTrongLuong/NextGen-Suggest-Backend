package com.ecommerce.nivora.order.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.order.dto.request.CheckoutRequest;
import com.ecommerce.nivora.order.dto.response.OrderResponse;
import com.ecommerce.nivora.order.entity.Order;
import com.ecommerce.nivora.order.service.OrderService;
import com.ecommerce.nivora.security.utils.SecurityUtils;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @PostMapping("/checkout")
    @Operation(summary = "Place an order (checkout)")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully",
                        orderService.checkout(securityUtils.getCurrentUserId(), request)));
    }

    @GetMapping
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getUserOrders(securityUtils.getCurrentUserId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(securityUtils.getCurrentUserId(), id)));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByNumber(orderNumber)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Cancelled by customer") String reason) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                orderService.cancelOrder(securityUtils.getCurrentUserId(), id, reason)));
    }
}
