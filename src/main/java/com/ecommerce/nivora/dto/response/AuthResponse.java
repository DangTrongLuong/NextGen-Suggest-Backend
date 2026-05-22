package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserResponse user;

    @Builder.Default
    private String tokenType_ = "Bearer";
}
