package com.ecommerce.nivora.auth.service;

import com.ecommerce.nivora.auth.dto.request.LoginRequest;
import com.ecommerce.nivora.auth.dto.request.RefreshTokenRequest;
import com.ecommerce.nivora.auth.dto.request.RegisterRequest;
import com.ecommerce.nivora.auth.dto.response.AuthResponse;
import com.ecommerce.nivora.auth.entity.RefreshToken;
import com.ecommerce.nivora.auth.repository.RefreshTokenRepository;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.UnauthorizedException;
import com.ecommerce.nivora.exception.dto.request.BadRequestException;
import com.ecommerce.nivora.security.utils.JwtUtils;
import com.ecommerce.nivora.user.dto.response.UserResponse;
import com.ecommerce.nivora.user.entity.Role;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.RoleRepository;
import com.ecommerce.nivora.user.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        Role customerRole = roleRepository.findByName("customer")
                .orElseGet(() -> roleRepository.save(
                    Role.builder().name("customer").displayName("Customer").build()));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(customerRole))
                .isActive(true)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // Rotate: revoke old, issue new
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
            t.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(t);
        });
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(String idToken) {
        // Placeholder — full Google token verification requires google-auth-library
        // In production: verify idToken with Google, extract email/name/picture
        throw new BadRequestException("Google login via token not yet implemented. Use OAuth2 flow.");
    }

    // ── Helpers ──────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        String rawRefreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshExpiration() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        log.info("User {} has roles from DB: {}", user.getEmail(), roleNames);

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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
