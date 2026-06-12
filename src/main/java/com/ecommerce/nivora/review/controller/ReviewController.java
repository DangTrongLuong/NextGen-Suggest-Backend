package com.ecommerce.nivora.review.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.review.dto.request.ReviewRequest;
import com.ecommerce.nivora.review.dto.response.ReviewResponse;
import com.ecommerce.nivora.review.entity.Review;
import com.ecommerce.nivora.review.service.ReviewService;
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
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review endpoints")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getByProduct(productId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping
    @Operation(summary = "Create a review")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted",
                        reviewService.create(securityUtils.getCurrentUserId(), request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reviewService.delete(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}
