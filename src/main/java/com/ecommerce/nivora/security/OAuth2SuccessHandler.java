package com.ecommerce.nivora.security;

import com.ecommerce.nivora.security.utils.JwtUtils;
import com.ecommerce.nivora.user.entity.Role;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.RoleRepository;
import com.ecommerce.nivora.user.repository.UserRepository;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email     = extractEmail(oAuth2User);
        String name      = extractName(oAuth2User);
        String providerId = extractProviderId(oAuth2User);
        String avatarUrl  = extractAvatar(oAuth2User);

        if (email == null) {
            log.error("OAuth2 login failed: email not found in principal");
            response.sendRedirect(frontendUrl + "/login?error=email_not_found");
            return;
        }

        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role customerRole = roleRepository.findByName("customer")
                    .orElseThrow(() -> new IllegalStateException("Role 'customer' not found"));
            return User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .provider("google")
                    .providerId(providerId)
                    .avatarUrl(avatarUrl)
                    .emailVerifiedAt(LocalDateTime.now())
                    .isActive(true)
                    .roles(Set.of(customerRole))
                    .build();
        });

        if (user.getProvider() == null) {
            user.setProvider("google");
            user.setProviderId(providerId);
        }
        if (user.getAvatarUrl() == null && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        String accessToken  = jwtUtils.generateAccessToken(email);
        String refreshToken = jwtUtils.generateRefreshToken(email);

        // Encode user info vào Base64 JSON — frontend dùng luôn, không cần gọi thêm API
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id",          user.getId());
        userInfo.put("uuid",        user.getUuid());
        userInfo.put("name",        user.getName());
        userInfo.put("email",       user.getEmail());
        userInfo.put("avatar",      user.getAvatarUrl());
        userInfo.put("avatarUrl",   user.getAvatarUrl());
        userInfo.put("roles",       roleNames);
        userInfo.put("role",        roleNames.contains("admin") ? "admin" : "user");
        userInfo.put("isActive",    user.getIsActive());

        String userJson   = objectMapper.writeValueAsString(userInfo);
        String userBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(userJson.getBytes());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("user", userBase64)
                .build().toUriString();

        log.info("OAuth2 login success for: {}", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractEmail(OAuth2User user) {
        if (user instanceof OidcUser oidc) return oidc.getEmail();
        Object v = user.getAttribute("email");
        return v != null ? v.toString() : null;
    }

    private String extractName(OAuth2User user) {
        if (user instanceof OidcUser oidc) return oidc.getFullName();
        Object v = user.getAttribute("name");
        return v != null ? v.toString() : null;
    }

    private String extractProviderId(OAuth2User user) {
        if (user instanceof OidcUser oidc) return oidc.getSubject();
        Object v = user.getAttribute("sub");
        return v != null ? v.toString() : null;
    }

    private String extractAvatar(OAuth2User user) {
        if (user instanceof OidcUser oidc) return oidc.getPicture();
        Object v = user.getAttribute("picture");
        return v != null ? v.toString() : null;
    }
}
