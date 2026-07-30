package com.ecommerce.nivora.auth.service;

import com.ecommerce.nivora.auth.dto.request.LoginRequest;
import com.ecommerce.nivora.auth.dto.request.RefreshTokenRequest;
import com.ecommerce.nivora.auth.dto.request.RegisterRequest;
import com.ecommerce.nivora.auth.dto.response.AuthResponse;



import com.ecommerce.nivora.auth.dto.request.VerifyRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    AuthResponse googleLogin(String idToken);
    AuthResponse verifyCode(VerifyRequest request);
    void resendVerificationCode(String email);
}
