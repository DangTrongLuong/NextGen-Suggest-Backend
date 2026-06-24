package com.ecommerce.nivora.admin.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.dto.request.ProductRequest;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Products", description = "Admin product management")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private final ProductService productService;

    /**
     * Whitelist of JPA entity field names allowed in ORDER BY clauses.
     * Prevents SQL injection through dynamic sorting parameters.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "originalPrice", "discountPercent",
            "createdAt", "updatedAt", "averageRating", "reviewCount", "sold", "active"
    );

    @GetMapping
    @Operation(summary = "Get all products (including inactive) with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // --- Security: whitelist sort field to prevent ORDER BY injection ---
        String safeSort = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, safeSort);

        // --- Bound pagination to prevent resource exhaustion ---
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 200);

        // --- Cap search keyword length ---
        String validSearch = (search != null && search.length() > 200) ? search.substring(0, 200) : search;

        return ResponseEntity.ok(ApiResponse.success(productService.getAllForAdmin(
                categoryId, validSearch,
                PageRequest.of(validPage, validSize, sort))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getByIdForAdmin(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}
