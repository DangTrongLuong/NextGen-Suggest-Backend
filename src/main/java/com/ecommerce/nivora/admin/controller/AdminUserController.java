package com.ecommerce.nivora.admin.controller;

import com.ecommerce.nivora.admin.dto.response.AdminUserResponse;
import com.ecommerce.nivora.admin.service.AdminUserService;
import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.user.entity.User;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin user/customer management")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.getAllUsers(search,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUserById(id)));
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("User updated", adminUserService.setActive(id, active)));
    }
}
