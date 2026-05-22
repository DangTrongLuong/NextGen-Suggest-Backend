package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.request.CartItemRequest;
import com.ecommerce.nivora.dto.response.CartResponse;
import com.ecommerce.nivora.service.CartService;
import com.ecommerce.nivora.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(securityUtils.getCurrentUserId())));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to cart",
                cartService.addItem(securityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/items/{variantId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long variantId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(ApiResponse.success("Cart updated",
                cartService.updateItem(securityUtils.getCurrentUserId(), variantId, quantity)));
    }

    @DeleteMapping("/items/{variantId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed",
                cartService.removeItem(securityUtils.getCurrentUserId(), variantId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
