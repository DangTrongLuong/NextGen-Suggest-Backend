package com.ecommerce.nivora.product.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.dto.response.ProductVariantResponse;
import com.ecommerce.nivora.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping({"/api/v1/products", "/api/products"})
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog endpoints")
public class ProductController {

    private final ProductService productService;

    /**
     * Whitelist of JPA entity field names allowed in ORDER BY clauses.
     * Any value not in this set is silently replaced with the default (createdAt)
     * to prevent SQL injection through dynamic sorting.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "originalPrice", "discountPercent",
            "createdAt", "updatedAt", "averageRating", "reviewCount", "sold"
    );

    @GetMapping
    @Operation(summary = "Get all products with filters and pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String categoryMainSlug,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // --- Security: whitelist sort field to prevent ORDER BY injection ---
        String safeSort = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, safeSort);

        // --- Bound pagination to prevent resource exhaustion ---
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 100);

        // --- Cap search keyword length ---
        String validSearch = (search != null && search.length() > 200) ? search.substring(0, 200) : search;

        return ResponseEntity.ok(ApiResponse.success(
                productService.getAll(categoryId, categorySlug, categoryMainSlug, brand,
                        minPrice, maxPrice, minRating, validSearch,
                        PageRequest.of(validPage, validSize, sort))));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug (compat)")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlugCompat(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getBySlug(slug)));
    }

    @GetMapping("/{slug:\\d*[a-zA-Z-][a-zA-Z0-9-]*}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getBySlug(slug)));
    }

    @GetMapping("/{id:\\d+}/variants")
    @Operation(summary = "Get product variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getVariants(id)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeatured()));
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best seller products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getBestSellers() {
        return ResponseEntity.ok(ApiResponse.success(productService.getBestSellers()));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewArrivals() {
        return ResponseEntity.ok(ApiResponse.success(productService.getNewArrivals()));
    }

    @GetMapping("/{id:\\d+}/related")
    @Operation(summary = "Get related products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelated(id, limit)));
    }
}
