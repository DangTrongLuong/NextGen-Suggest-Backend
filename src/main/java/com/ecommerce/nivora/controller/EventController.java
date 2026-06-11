package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.request.UserBehaviorEventRequest;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.repository.UserRepository;
import com.ecommerce.nivora.service.UserBehaviorEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/events", "/api/events"})
@RequiredArgsConstructor
@Tag(name = "Events", description = "User behavior event tracking endpoints")
public class EventController {

    private final UserBehaviorEventService behaviorEventService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Track user behavior event")
    public ResponseEntity<ApiResponse<Void>> trackEvent(@Valid @RequestBody UserBehaviorEventRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                User user = userRepository.findActiveByEmail(auth.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            } catch (Exception e) {
                // Ignore and proceed as guest
            }
        }

        behaviorEventService.trackEvent(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Event tracked successfully", null));
    }
}
