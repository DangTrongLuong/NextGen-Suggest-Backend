package com.ecommerce.nivora.admin.dto.response;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserResponse {
    private Long id;
    private String uuid;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String role;
    private Set<String> roles;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Long orderCount;
    private BigDecimal totalSpent;
}
