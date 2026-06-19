package com.ecommerce.nivora.category.controller;

import com.ecommerce.nivora.category.dto.response.CategoryMainResponse;
import com.ecommerce.nivora.category.service.CategoryMainService;
import com.ecommerce.nivora.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/category-mains", "/api/category-mains"})
@RequiredArgsConstructor
@Tag(name = "Category Mains", description = "Main category endpoints")
public class CategoryMainController {

    private final CategoryMainService categoryMainService;

    @GetMapping
    @Operation(summary = "Get all category mains")
    public ResponseEntity<ApiResponse<List<CategoryMainResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryMainService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category main by ID")
    public ResponseEntity<ApiResponse<CategoryMainResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryMainService.getById(id)));
    }
}
