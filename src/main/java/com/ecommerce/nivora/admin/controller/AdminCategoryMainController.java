package com.ecommerce.nivora.admin.controller;

import com.ecommerce.nivora.category.dto.request.CategoryMainRequest;
import com.ecommerce.nivora.category.dto.response.CategoryMainResponse;
import com.ecommerce.nivora.category.service.CategoryMainService;
import com.ecommerce.nivora.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/category-mains")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Category Mains", description = "Admin main category management")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryMainController {

    private final CategoryMainService categoryMainService;

    @GetMapping
    @Operation(summary = "Get all category mains for admin")
    public ResponseEntity<ApiResponse<List<CategoryMainResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryMainService.getAllForAdmin()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category main by ID for admin")
    public ResponseEntity<ApiResponse<CategoryMainResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryMainService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a category main")
    public ResponseEntity<ApiResponse<CategoryMainResponse>> create(@Valid @RequestBody CategoryMainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CategoryMain created", categoryMainService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category main")
    public ResponseEntity<ApiResponse<CategoryMainResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryMainRequest request) {
        return ResponseEntity.ok(ApiResponse.success("CategoryMain updated", categoryMainService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category main")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryMainService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("CategoryMain deleted", null));
    }
}
