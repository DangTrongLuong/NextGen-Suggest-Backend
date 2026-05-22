package com.ecommerce.nivora.utils;

import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.exception.UnauthorizedException;
import com.ecommerce.nivora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return auth.getName();
    }

    public User getCurrentUser() {
        return userRepository.findActiveByEmail(getCurrentEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
