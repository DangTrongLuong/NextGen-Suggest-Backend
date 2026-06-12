package com.ecommerce.nivora.recommendation.service;

import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.recommendation.dto.response.HomepageRecommendationResponse;


import java.util.List;

public interface RecommendationService {
    HomepageRecommendationResponse getHomepageRecommendations(String sessionId, Long userId, List<Long> guestViewedIds);
    List<ProductResponse> getRecommendationsByLocation(String location, int limit);
}
