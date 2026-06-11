package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.request.FastApiHybridRecommendRequest;
import com.ecommerce.nivora.dto.response.*;
import com.ecommerce.nivora.entity.RecommendationCache;
import com.ecommerce.nivora.entity.Wishlist;
import com.ecommerce.nivora.repository.*;
import com.ecommerce.nivora.service.ProductService;
import com.ecommerce.nivora.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RestTemplate restTemplate;
    private final ProductService productService;
    private final RecommendationCacheRepository cacheRepository;
    private final UserBehaviorEventRepository behaviorEventRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    @Transactional(readOnly = true)
    public HomepageRecommendationResponse getHomepageRecommendations(String sessionId, Long userId, List<Long> guestViewedIds) {
        log.info("Generating homepage recommendations for user={}, session={}, guestViewedCount={}", 
                userId, sessionId, guestViewedIds != null ? guestViewedIds.size() : 0);

        // 1. Lấy Featured, BestSellers, NewArrivals có sẵn từ DB (đã được cache hoặc tối ưu)
        List<ProductResponse> featured = productService.getFeatured();
        List<ProductResponse> bestSellers = productService.getBestSellers();
        List<ProductResponse> newArrivals = productService.getNewArrivals();

        // 2. Lấy danh sách Trending Today từ Cache
        List<ProductResponse> trending = getTrendingFromCache("trending_today");
        if (trending.isEmpty()) {
            trending = bestSellers; // Fallback
        }

        // 3. Lấy Tiếp tục xem (Recent Viewed)
        List<ProductResponse> recent = getRecentViewed(sessionId, userId, guestViewedIds);

        // 4. Lấy AI Recommendations (Personalized) & You May Need (Cross-sell)
        List<ProductResponse> personal = new ArrayList<>();
        List<ProductResponse> youMayNeed = new ArrayList<>();
        
        try {
            // Chuẩn bị dữ liệu gửi sang FastAPI
            List<String> viewedUuids = recent.stream().map(ProductResponse::getUuid).collect(Collectors.toList());
            List<String> wishlistUuids = getWishlistUuids(userId);
            List<String> purchasedUuids = getPurchasedUuids(userId);
            List<String> popularUuids = trending.stream().map(ProductResponse::getUuid).collect(Collectors.toList());

            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/recommend/hybrid")
                    .build()
                    .toUriString();

            FastApiHybridRecommendRequest apiRequest = FastApiHybridRecommendRequest.builder()
                    .viewed_product_ids(viewedUuids)
                    .wishlist_product_ids(wishlistUuids)
                    .purchased_product_ids(purchasedUuids)
                    .popular_product_ids(popularUuids)
                    .top_k(12)
                    .exclude_viewed(true)
                    .build();

            log.info("Requesting hybrid recommendations from FastAPI: {}", targetUrl);
            FastApiRecommendResponse apiResponse = restTemplate.postForObject(targetUrl, apiRequest, FastApiRecommendResponse.class);

            if (apiResponse != null && apiResponse.getRecommendations() != null && !apiResponse.getRecommendations().isEmpty()) {
                List<String> recommendedUuids = apiResponse.getRecommendations().stream()
                        .map(FastApiProductResult::getProductId)
                        .collect(Collectors.toList());

                Map<String, ProductResponse> hydratedProducts = productService.getActiveProductsBySkus(recommendedUuids);
                
                List<ProductResponse> allRecs = recommendedUuids.stream()
                        .map(hydratedProducts::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // Phân bổ: 8 cái đầu cho Personal, 4 cái sau cho You May Need
                if (allRecs.size() > 8) {
                    personal.addAll(allRecs.subList(0, 8));
                    youMayNeed.addAll(allRecs.subList(8, allRecs.size()));
                } else {
                    personal.addAll(allRecs);
                    // Nếu ít hơn 8, lấy thêm bestSellers bù vào You May Need
                    youMayNeed.addAll(bestSellers.stream()
                            .filter(p -> !personal.contains(p))
                            .limit(4)
                            .collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch hybrid recommendations from FastAPI", e);
        }

        // Fallbacks nếu FastAPI gặp lỗi hoặc không trả về kết quả
        if (personal.isEmpty()) {
            personal.addAll(featured);
        }
        if (youMayNeed.isEmpty()) {
            youMayNeed.addAll(bestSellers.stream().limit(4).collect(Collectors.toList()));
        }

        return HomepageRecommendationResponse.builder()
                .featured(featured)
                .bestSellers(bestSellers)
                .newArrivals(newArrivals)
                .trending(trending)
                .recent(recent)
                .personal(personal)
                .youMayNeed(youMayNeed)
                .build();
    }

    private List<ProductResponse> getTrendingFromCache(String recType) {
        Optional<RecommendationCache> cacheOpt = cacheRepository.findByUserIdAndRecType(null, recType);
        if (cacheOpt.isPresent()) {
            String productIdsStr = cacheOpt.get().getProductIds();
            if (productIdsStr != null && !productIdsStr.isBlank()) {
                try {
                    List<Long> ids = Arrays.stream(productIdsStr.split(","))
                            .map(String::trim)
                            .map(Long::parseLong)
                            .collect(Collectors.toList());
                    
                    return productService.getProductsByIds(ids);
                } catch (Exception e) {
                    log.error("Error parsing trending cache product IDs: {}", productIdsStr, e);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<ProductResponse> getRecentViewed(String sessionId, Long userId, List<Long> guestViewedIds) {
        if (userId != null) {
            // Lấy từ user behavior logs ở Database
            List<String> recentUuids = behaviorEventRepository.findRecentViewedProductUuidsByUserId(
                    userId, PageRequest.of(0, 10));
            if (!recentUuids.isEmpty()) {
                Map<String, ProductResponse> hydrated = productService.getActiveProductsBySkus(recentUuids);
                return recentUuids.stream()
                        .map(hydrated::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }
        
        // Nếu không có trong DB hoặc là Guest User
        if (guestViewedIds != null && !guestViewedIds.isEmpty()) {
            return productService.getProductsByIds(guestViewedIds);
        }

        // Lấy theo session trong DB làm dự phòng
        if (sessionId != null && !sessionId.isBlank()) {
            List<String> sessionUuids = behaviorEventRepository.findRecentViewedProductUuidsBySessionId(
                    sessionId, PageRequest.of(0, 10));
            if (!sessionUuids.isEmpty()) {
                Map<String, ProductResponse> hydrated = productService.getActiveProductsBySkus(sessionUuids);
                return sessionUuids.stream()
                        .map(hydrated::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> getWishlistUuids(Long userId) {
        if (userId == null) return Collections.emptyList();
        return wishlistRepository.findByUserId(userId)
                .map(w -> w.getItems().stream()
                        .map(item -> item.getProduct().getUuid())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<String> getPurchasedUuids(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return orderRepository.findPurchasedProductUuidsByUserId(userId);
        } catch (Exception e) {
            log.error("Error fetching purchased product UUIDs for user={}", userId, e);
            return Collections.emptyList();
        }
    }
}
