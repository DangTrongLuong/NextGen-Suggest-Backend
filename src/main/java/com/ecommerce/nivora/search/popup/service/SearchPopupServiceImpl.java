package com.ecommerce.nivora.search.popup.service;

import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.category.service.CategoryService;
import com.ecommerce.nivora.event.repository.UserBehaviorEventRepository;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.service.ProductService;
import com.ecommerce.nivora.search.popup.dto.AutocompleteResponse;
import com.ecommerce.nivora.search.popup.dto.SearchPopupResponse;
import com.ecommerce.nivora.search.repository.SearchLogRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchPopupServiceImpl implements SearchPopupService {

    private final StringRedisTemplate redisTemplate;
    private final SearchLogRepository searchLogRepository;
    private final UserBehaviorEventRepository behaviorEventRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    private static final String HISTORY_KEY_PREFIX = "nivora:search:history:";
    private static final String RECENT_VIEWED_KEY_PREFIX = "nivora:user:recent_viewed:";
    private static final String TRENDING_KEY = "nivora:search:trending";

    @Override
    @Transactional(readOnly = true)
    public SearchPopupResponse getSearchPopup(Long userId, String sessionId) {
        log.info("Fetching search popup for user={}, session={}", userId, sessionId);
        String userOrSessionId = (userId != null) ? String.valueOf(userId) : sessionId;

        // 1. Fetch sections in parallel using CompletableFuture
        CompletableFuture<List<String>> historyFuture = CompletableFuture.supplyAsync(() -> getHistory(userId, sessionId, userOrSessionId));
        CompletableFuture<List<ProductResponse>> recentFuture = CompletableFuture.supplyAsync(() -> getRecentViewed(userId, sessionId, userOrSessionId));
        CompletableFuture<List<String>> trendingFuture = CompletableFuture.supplyAsync(this::getTrending);
        CompletableFuture<List<CategoryResponse>> categoriesFuture = CompletableFuture.supplyAsync(this::getPopularCategories);

        CompletableFuture.allOf(historyFuture, recentFuture, trendingFuture, categoriesFuture).join();

        List<String> history = historyFuture.join();
        List<ProductResponse> recent = recentFuture.join();
        List<String> trending = trendingFuture.join();
        List<CategoryResponse> categories = categoriesFuture.join();

        // 2. Fetch related queries from FastAPI based on last viewed product category
        List<String> related = getRelatedSearches(recent);

        return SearchPopupResponse.builder()
                .searchHistory(history)
                .recentlyViewed(recent)
                .trendingSearches(trending)
                .popularCategories(categories)
                .relatedSearches(related)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AutocompleteResponse getSuggestions(String query, Long userId, String sessionId) {
        log.info("Fetching autocomplete suggestions for query='{}', user={}, session={}", query, userId, sessionId);
        if (query == null || query.trim().length() < 2) {
            return AutocompleteResponse.builder().query(query).suggestions(Collections.emptyList()).build();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/ai/semantic-suggestions")
                    .queryParam("q", query.trim())
                    .queryParam("userId", userId != null ? String.valueOf(userId) : "")
                    .queryParam("limit", 8)
                    .build()
                    .toUriString();

            FastApiAutocompleteResponse apiResponse = restTemplate.getForObject(url, FastApiAutocompleteResponse.class);
            if (apiResponse != null && apiResponse.getSuggestions() != null) {
                List<AutocompleteResponse.Suggestion> suggestions = apiResponse.getSuggestions().stream()
                        .map(s -> {
                            CategoryResponse categoryDto = null;
                            if (s.getCategoryId() != null) {
                                try {
                                    categoryDto = categoryService.getById(s.getCategoryId());
                                } catch (Exception e) {
                                    // Category not found or inactive, ignored
                                }
                            }
                            return AutocompleteResponse.Suggestion.builder()
                                    .keyword(s.getKeyword())
                                    .type(s.getType())
                                    .category(categoryDto)
                                    .build();
                        })
                        .collect(Collectors.toList());

                return AutocompleteResponse.builder()
                        .query(query)
                        .suggestions(suggestions)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch autocomplete suggestions from FastAPI", e);
        }

        // Fallback local matching
        return getFallbackSuggestions(query, userId, sessionId);
    }

    @Override
    @Transactional
    public void deleteHistory(Long userId, String sessionId, String keyword) {
        log.info("Deleting search history for user={}, session={}, keyword={}", userId, sessionId, keyword);
        try {
            if (keyword != null && !keyword.isBlank()) {
                String cleanKw = keyword.trim();
                if (userId != null) {
                    searchLogRepository.deleteByUserIdAndQueryText(userId, cleanKw);
                } else if (sessionId != null && !sessionId.isBlank()) {
                    searchLogRepository.deleteBySessionIdAndQueryText(sessionId, cleanKw);
                }
            } else {
                if (userId != null) {
                    searchLogRepository.deleteByUserId(userId);
                } else if (sessionId != null && !sessionId.isBlank()) {
                    searchLogRepository.deleteBySessionId(sessionId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete search history from MySQL", e);
        }
    }

    private List<String> getHistory(Long userId, String sessionId, String userOrSessionId) {
        try {
            List<String> dbHistory;
            if (userId != null) {
                dbHistory = searchLogRepository.findRecentQueriesByUserId(userId, PageRequest.of(0, 5));
            } else if (sessionId != null && !sessionId.isBlank()) {
                dbHistory = searchLogRepository.findRecentQueriesBySessionId(sessionId, PageRequest.of(0, 5));
            } else {
                dbHistory = Collections.emptyList();
            }
            return dbHistory;
        } catch (Exception e) {
            log.error("Error reading search history from MySQL", e);
            return Collections.emptyList();
        }
    }

    private List<ProductResponse> getRecentViewed(Long userId, String sessionId, String userOrSessionId) {
        String key = RECENT_VIEWED_KEY_PREFIX + userOrSessionId;
        try {
            Set<String> cached = redisTemplate.opsForZSet().reverseRange(key, 0, 5);
            if (cached != null && !cached.isEmpty()) {
                List<Long> ids = cached.stream().map(Long::parseLong).collect(Collectors.toList());
                return productService.getProductsByIds(ids);
            }
            log.info("Redis recent viewed miss. Fetching from MySQL for: {}", userOrSessionId);
            List<String> uuids;
            if (userId != null) {
                uuids = behaviorEventRepository.findRecentViewedProductUuidsByUserId(userId, PageRequest.of(0, 6));
            } else if (sessionId != null && !sessionId.isBlank()) {
                uuids = behaviorEventRepository.findRecentViewedProductUuidsBySessionId(sessionId, PageRequest.of(0, 6));
            } else {
                uuids = Collections.emptyList();
            }

            if (!uuids.isEmpty()) {
                Map<String, ProductResponse> hydrated = productService.getActiveProductsByUuids(uuids);
                List<ProductResponse> products = uuids.stream()
                        .map(hydrated::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                long now = System.currentTimeMillis();
                for (int i = 0; i < products.size(); i++) {
                    redisTemplate.opsForZSet().add(key, String.valueOf(products.get(i).getId()), now - (i * 1000));
                }
                redisTemplate.expire(key, Duration.ofDays(7));
                return products;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching recently viewed products", e);
            return Collections.emptyList();
        }
    }

    private List<String> getTrending() {
        try {
            Set<String> cached = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, 9);
            if (cached != null && !cached.isEmpty()) {
                return new ArrayList<>(cached);
            }
            log.info("Redis trending miss. Fetching from MySQL");
            List<Object[]> dbTrending = searchLogRepository.findTopQueriesSince(LocalDateTime.now().minusHours(24), PageRequest.of(0, 10));
            List<String> trending = dbTrending.stream()
                    .map(row -> (String) row[0])
                    .collect(Collectors.toList());

            if (!trending.isEmpty()) {
                long now = System.currentTimeMillis();
                for (int i = 0; i < trending.size(); i++) {
                    redisTemplate.opsForZSet().add(TRENDING_KEY, trending.get(i), 10.0 - i);
                }
            }
            return trending;
        } catch (Exception e) {
            log.error("Error fetching trending queries", e);
            return Collections.emptyList();
        }
    }

    private List<CategoryResponse> getPopularCategories() {
        try {
            List<CategoryResponse> categories = categoryService.getAll(null, true);
            if (categories != null) {
                return categories.stream().limit(6).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching categories for popular list", e);
        }
        return Collections.emptyList();
    }

    private List<String> getRelatedSearches(List<ProductResponse> recent) {
        if (recent == null || recent.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String uuid = recent.get(0).getUuid();
            String url = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/ai/related-queries")
                    .queryParam("productUuid", uuid)
                    .queryParam("limit", 5)
                    .build()
                    .toUriString();

            String[] response = restTemplate.getForObject(url, String[].class);
            if (response != null) {
                return Arrays.asList(response);
            }
        } catch (Exception e) {
            log.error("Failed to fetch related searches from FastAPI", e);
        }
        return Collections.emptyList();
    }

    private AutocompleteResponse getFallbackSuggestions(String query, Long userId, String sessionId) {
        log.info("Executing local suggestion matching fallback for query: {}", query);
        List<AutocompleteResponse.Suggestion> suggestions = new ArrayList<>();

        // 1. User History match
        String userOrSessionId = (userId != null) ? String.valueOf(userId) : sessionId;
        String historyKey = HISTORY_KEY_PREFIX + userOrSessionId;
        try {
            Set<String> cachedHistory = redisTemplate.opsForZSet().reverseRange(historyKey, 0, -1);
            if (cachedHistory != null) {
                String cleanQuery = query.toLowerCase().trim();
                cachedHistory.stream()
                        .filter(h -> h.toLowerCase().contains(cleanQuery))
                        .limit(3)
                        .forEach(h -> suggestions.add(AutocompleteResponse.Suggestion.builder()
                                .keyword(h)
                                .type("HISTORY")
                                .build()));
            }
        } catch (Exception e) {
            // Ignored
        }

        // 2. Parent categories match
        try {
            List<CategoryResponse> categories = categoryService.getAll(query, true);
            if (categories != null) {
                categories.stream().limit(3).forEach(c -> suggestions.add(AutocompleteResponse.Suggestion.builder()
                        .keyword(c.getName())
                        .type("AI_SUGGESTION")
                        .category(c)
                        .build()));
            }
        } catch (Exception e) {
            // Ignored
        }

        Set<String> seen = new HashSet<>();
        List<AutocompleteResponse.Suggestion> deduped = suggestions.stream()
                .filter(s -> seen.add(s.getKeyword().toLowerCase()))
                .limit(8)
                .collect(Collectors.toList());

        return AutocompleteResponse.builder()
                .query(query)
                .suggestions(deduped)
                .build();
    }

    // Helper classes for FastAPI deserialization
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiAutocompleteResponse {
        private String query;
        private List<FastApiSuggestion> suggestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastApiSuggestion {
        private String keyword;
        private String type;
        private Long categoryId;
    }
}
