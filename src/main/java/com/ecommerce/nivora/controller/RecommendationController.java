package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.HomepageRecommendationResponse;
import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.repository.UserRepository;
import com.ecommerce.nivora.service.RecommendationService;
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
}
