package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.request.LoginRequest;
import com.ecommerce.nivora.dto.request.RefreshTokenRequest;
import com.ecommerce.nivora.dto.request.RegisterRequest;
import com.ecommerce.nivora.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    AuthResponse googleLogin(String idToken);
}
