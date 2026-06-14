package com.ecommerce.nivora.recommendation.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.recommendation.dto.response.HomepageRecommendationResponse;
import com.ecommerce.nivora.recommendation.service.RecommendationService;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/recommendations", "/api/recommendations"})
@RequiredArgsConstructor
@Tag(name = "Recommendation", description = "Product Recommendation endpoints")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping("/homepage")
    @Operation(summary = "Get aggregated product recommendations for the homepage")
    public ResponseEntity<ApiResponse<HomepageRecommendationResponse>> getHomepageRecommendations(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) List<Long> guestViewedIds
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                User user = userRepository.findActiveByEmail(auth.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            } catch (Exception e) {
                // Safe fallback to guest mode
            }
        }

        List<Long> viewedIds = guestViewedIds != null ? guestViewedIds : new ArrayList<>();
        HomepageRecommendationResponse response = recommendationService.getHomepageRecommendations(sessionId, userId, viewedIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/location")
    @Operation(summary = "Get regional product recommendations based on customer orders in that location")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRecommendationsByLocation(
            @RequestParam String location,
            @RequestParam(defaultValue = "8") int limit
    ) {
        List<ProductResponse> response = recommendationService.getRecommendationsByLocation(location, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/location-based")
    @Operation(summary = "Get location-based product recommendations for current authenticated user or nationwide")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLocationBasedRecommendations(
            @RequestParam(defaultValue = "8") int limit
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                User user = userRepository.findActiveByEmail(auth.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        List<ProductResponse> response = recommendationService.getLocationBasedRecommendations(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
