package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.CategoryResponse;
import com.ecommerce.nivora.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAll()));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree (nested)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTree()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }
}
