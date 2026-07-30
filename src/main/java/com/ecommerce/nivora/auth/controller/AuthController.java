package com.ecommerce.nivora.auth.controller;

import com.ecommerce.nivora.auth.dto.request.LoginRequest;
import com.ecommerce.nivora.auth.dto.request.RefreshTokenRequest;
import com.ecommerce.nivora.auth.dto.request.RegisterRequest;
import com.ecommerce.nivora.auth.dto.request.VerifyRequest;
import com.ecommerce.nivora.auth.dto.response.AuthResponse;
import com.ecommerce.nivora.auth.service.AuthService;
import com.ecommerce.nivora.common.dto.response.ApiResponse;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify account with 8-digit code")
    public ResponseEntity<ApiResponse<AuthResponse>> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully", authService.verifyCode(request)));
    }

    @PostMapping("/resend-code")
    @Operation(summary = "Resend verification code")
    public ResponseEntity<ApiResponse<Void>> resendCode(@RequestParam String email) {
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent successfully", null));
    }
}
