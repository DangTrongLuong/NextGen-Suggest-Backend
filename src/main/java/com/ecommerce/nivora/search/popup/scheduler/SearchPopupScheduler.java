package com.ecommerce.nivora.search.popup.scheduler;

import com.ecommerce.nivora.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchPopupScheduler {

    private final SearchLogRepository searchLogRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String TRENDING_KEY = "nivora:search:trending";

    // Run 15 seconds after startup, then every 1 hour
    @Scheduled(initialDelay = 15000, fixedRate = 3600000)
    @Transactional(readOnly = true)
    public void calculateTrendingSearches() {
        log.info("Starting scheduled Trending Searches calculation...");
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Object[]> topQueries = searchLogRepository.findTopQueriesSince(since, PageRequest.of(0, 10));

            if (topQueries != null && !topQueries.isEmpty()) {
                redisTemplate.delete(TRENDING_KEY);
                for (Object[] row : topQueries) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        String queryText = row[0].toString().trim();
                        Double count = ((Long) row[1]).doubleValue();
                        redisTemplate.opsForZSet().add(TRENDING_KEY, queryText, count);
                    }
                }
                log.info("Updated Redis trending searches ZSET with {} queries", topQueries.size());
            } else {
                log.warn("No search queries found in the last 24 hours to aggregate trending.");
            }
        } catch (Exception e) {
            log.error("Error calculating trending searches", e);
        }
    }
}
