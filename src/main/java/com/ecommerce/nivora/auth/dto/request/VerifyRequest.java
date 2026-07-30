package com.ecommerce.nivora.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{8}", message = "Verification code must be exactly 8 digits")
    private String code;
}
