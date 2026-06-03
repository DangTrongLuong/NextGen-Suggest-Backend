package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.service.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheServiceImpl implements RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.search-ttl-minutes:60}")
    private long cacheTtlMinutes;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    public List<ProductResponse> getCachedSearchResults(String query, boolean rerank) {
        String key = buildCacheKey(query, rerank);
        try {
            String cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                log.info("[REDIS] HIT {}", key);
                return objectMapper.readValue(cachedData, new TypeReference<List<ProductResponse>>() {});
            }
            log.info("[REDIS] MISS {}", key);
        } catch (Exception e) {
            log.warn("[REDIS] Error reading from cache for key: {}. Error: {}", key, e.getMessage());
        }
        return null;
    }

    @Override
    public void cacheSearchResults(String query, boolean rerank, List<ProductResponse> results) {
        if (results == null) return;
        String key = buildCacheKey(query, rerank);
        try {
            String jsonString = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(key, jsonString, cacheTtlMinutes, TimeUnit.MINUTES);
            log.info("[REDIS] SAVE {}", key);
        } catch (Exception e) {
            log.warn("[REDIS] Error writing to cache for key: {}. Error: {}", key, e.getMessage());
        }
    }

    private String buildCacheKey(String query, boolean rerank) {
        String normalized = normalizeKeyword(query);
        return "search:" + normalized + ":" + rerank;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return "";
        
        // 1. Trim and lower case
        String normalized = keyword.trim().toLowerCase();
        
        // 2. Normalize to NFD form to separate base letters from diacritics
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        
        // 3. Remove all diacritical marks
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        
        // 4. Manually convert 'đ' to 'd'
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        
        // 5. Replace multiple whitespace characters with a single space
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
}
