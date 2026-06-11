package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.HomepageRecommendationResponse;
import java.util.List;

public interface RecommendationService {
    HomepageRecommendationResponse getHomepageRecommendations(String sessionId, Long userId, List<Long> guestViewedIds);
}
