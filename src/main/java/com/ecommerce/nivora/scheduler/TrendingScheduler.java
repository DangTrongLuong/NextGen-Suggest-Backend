package com.ecommerce.nivora.scheduler;

import com.ecommerce.nivora.entity.RecommendationCache;
import com.ecommerce.nivora.repository.RecommendationCacheRepository;
import com.ecommerce.nivora.repository.UserBehaviorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingScheduler {

    private final UserBehaviorEventRepository behaviorEventRepository;
    private final RecommendationCacheRepository cacheRepository;

    // Chạy 10 giây sau khi start app, và cứ mỗi 1 tiếng sau đó
    @Scheduled(initialDelay = 10000, fixedRate = 3600000)
    @Transactional
    public void calculateTrendingProducts() {
        log.info("Starting scheduled Trending Products calculation...");
        
        try {
            // 1. Tính toán Trending trong 24 giờ qua (Trending Today)
            LocalDateTime past24Hours = LocalDateTime.now().minusHours(24);
            List<Object[]> trendingTodayResults = behaviorEventRepository.findTrendingProductsSince(
                    past24Hours, PageRequest.of(0, 30));
            
            saveTrendingCache("trending_today", trendingTodayResults);

            // 2. Tính toán Trending trong 7 ngày qua (Trending Week)
            LocalDateTime past7Days = LocalDateTime.now().minusDays(7);
            List<Object[]> trendingWeekResults = behaviorEventRepository.findTrendingProductsSince(
                    past7Days, PageRequest.of(0, 30));
            
            saveTrendingCache("trending_week", trendingWeekResults);

            log.info("Trending Products calculation completed successfully.");
        } catch (Exception e) {
            log.error("Error calculating trending products", e);
        }
    }

    private void saveTrendingCache(String recType, List<Object[]> queryResults) {
        if (queryResults == null || queryResults.isEmpty()) {
            log.warn("No trending data found for type: {}", recType);
            return;
        }

        // Tạo chuỗi ID ngăn cách bằng dấu phẩy
        String productIds = queryResults.stream()
                .map(row -> String.valueOf(row[0])) // row[0] là product_id
                .collect(Collectors.joining(","));

        // Tìm cache cũ hoặc tạo mới
        RecommendationCache cache = cacheRepository.findByUserIdAndRecType(null, recType)
                .orElse(RecommendationCache.builder()
                        .recType(recType)
                        .user(null)
                        .sessionId(null)
                        .build());

        cache.setProductIds(productIds);
        cacheRepository.save(cache);
        
        log.info("Saved trending cache type={} with {} products: [{}]", recType, queryResults.size(), productIds);
    }
}
