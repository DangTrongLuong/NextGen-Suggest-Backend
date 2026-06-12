package com.ecommerce.nivora.auth.service;

import com.ecommerce.nivora.auth.dto.request.LoginRequest;
import com.ecommerce.nivora.auth.dto.request.RefreshTokenRequest;
import com.ecommerce.nivora.auth.dto.request.RegisterRequest;
import com.ecommerce.nivora.auth.dto.response.AuthResponse;



public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    AuthResponse googleLogin(String idToken);
}
