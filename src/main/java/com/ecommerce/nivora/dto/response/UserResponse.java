package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
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
    private LocalDateTime emailVerifiedAt;

    private Integer age;
    private String gender;
    private String occupation;
    private java.math.BigDecimal income;
    private String maritalStatus;
}
