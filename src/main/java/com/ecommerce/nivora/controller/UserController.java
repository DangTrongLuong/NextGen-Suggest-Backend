package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.UserResponse;
import com.ecommerce.nivora.entity.Role;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(toResponse(user)));
    }

    private UserResponse toResponse(User user) {
        var roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .role(roleNames.contains("admin") ? "admin" : "user")
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .build();
    }
}
