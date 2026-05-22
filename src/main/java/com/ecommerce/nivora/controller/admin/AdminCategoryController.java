package com.ecommerce.nivora.controller.admin;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.request.CategoryRequest;
import com.ecommerce.nivora.dto.response.CategoryResponse;
import com.ecommerce.nivora.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Categories", description = "Admin category management")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories (including inactive)")
    public ResponseEntity<ApiResponse<java.util.List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllForAdmin()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
