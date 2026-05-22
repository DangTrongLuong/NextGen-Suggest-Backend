package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.service.WishlistService;
import com.ecommerce.nivora.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist endpoints")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get current user's wishlist")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getWishlist() {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.getWishlist(securityUtils.getCurrentUserId())));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<Void>> add(@PathVariable Long productId) {
        wishlistService.addToWishlist(securityUtils.getCurrentUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", null));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(securityUtils.getCurrentUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", null));
    }

    @GetMapping("/{productId}/check")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> check(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.isInWishlist(securityUtils.getCurrentUserId(), productId)));
    }
}
