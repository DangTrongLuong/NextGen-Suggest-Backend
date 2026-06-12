package com.ecommerce.nivora.category.controller;

import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.category.service.CategoryService;
import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.product.entity.Product;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/categories", "/api/categories"})
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get active categories (with optional search, pagination, and parent-only filtering)")
    public ResponseEntity<ApiResponse<?>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean parentOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return ResponseEntity.ok(ApiResponse.success(categoryService.getPaginated(search, parentOnly, page, size)));
        }
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAll(search, parentOnly)));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree (nested)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTree()));
    }

    @GetMapping("/{slug}/breadcrumb")
    @Operation(summary = "Get breadcrumb by category slug")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getBreadcrumb(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getBreadcrumb(slug)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }
}
