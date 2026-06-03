package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.response.*;
import com.ecommerce.nivora.service.RedisCacheService;
import com.ecommerce.nivora.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final RestTemplate restTemplate;
    private final RedisCacheService redisCacheService;

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-z0-9]+");

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public List<ProductResponse> search(String query, boolean rerank) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Try to fetch from Redis Cache first
        List<ProductResponse> cachedResults = redisCacheService.getCachedSearchResults(query, rerank);
        if (cachedResults != null) {
            return cachedResults;
        }

        try {
            // Build the URL to call FastAPI (use_reranking, candidate_k, top_k parameters)
            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/search")
                    .queryParam("q", query)
                    .queryParam("use_reranking", rerank)
                    .queryParam("candidate_k", rerank ? 15 : 10)
                    .queryParam("top_k", 10)
                    .build()
                    .toUriString();

            log.info("Forwarding search query '{}' (rerank={}) to FastAPI: {}", query, rerank, targetUrl);

            // Make HTTP request to FastAPI
            FastApiSearchResponse apiResponse = restTemplate.getForObject(targetUrl, FastApiSearchResponse.class);

            if (apiResponse == null || apiResponse.getResults() == null || apiResponse.getResults().isEmpty()) {
                log.info("No semantic search results returned from FastAPI for query: {}", query);
                return Collections.emptyList();
            }

            log.info("Found {} results from FastAPI. Mapping directly to responses (without DB interaction).", apiResponse.getResults().size());

            // Map and return results in the EXACT same order returned by FastAPI without querying or saving to the database
            List<ProductResponse> results = apiResponse.getResults().stream()
                    .map(res -> {
                        long fakeId = (long) res.getProductId().hashCode();
                        if (fakeId < 0) fakeId = -fakeId;

                        String leafCat = getLeafCategory(res.getCategory());
                        String slug = SLUG_PATTERN.matcher(leafCat.toLowerCase()).replaceAll("-");

                        return ProductResponse.builder()
                                .id(fakeId)
                                .uuid(res.getProductId())
                                .sku(res.getProductId())
                                .name(res.getTitle())
                                .shortDescription(res.getDescription() != null && res.getDescription().length() > 490
                                        ? res.getDescription().substring(0, 490) + "..." : res.getDescription())
                                .description(res.getDescription())
                                .basePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice()) : BigDecimal.ZERO)
                                .comparePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice() * 1.2) : BigDecimal.ZERO)
                                .stock(100)
                                .ratingAvg(BigDecimal.valueOf(4.5))
                                .ratingCount(12)
                                .isActive(true)
                                .isNew(false)
                                .isFeatured(false)
                                .isBestSeller(false)
                                .imageUrl(res.getImageUrl())
                                .category(CategoryResponse.builder()
                                        .name(leafCat)
                                        .slug(slug)
                                        .build())
                                .images(res.getImageUrl() != null ? List.of(ProductImageResponse.builder()
                                        .id(fakeId)
                                        .url(res.getImageUrl())
                                        .isPrimary(true)
                                        .sortOrder(0)
                                        .build()) : Collections.emptyList())
                                .variants(List.of(ProductVariantResponse.builder()
                                        .id(fakeId)
                                        .sku(res.getProductId())
                                        .name(res.getTitle())
                                        .price(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice()) : BigDecimal.ZERO)
                                        .comparePrice(res.getPrice() != null ? BigDecimal.valueOf(res.getPrice() * 1.2) : BigDecimal.ZERO)
                                        .stock(100)
                                        .availableStock(100)
                                        .isActive(true)
                                        .build()))
                                .build();
                    })
                    .collect(Collectors.toList());

            // Save results to Redis cache
            redisCacheService.cacheSearchResults(query, rerank, results);

            return results;

        } catch (Exception e) {
            log.error("Failed to perform semantic search via FastAPI", e);
            return Collections.emptyList();
        }
    }

    private String getLeafCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return "Other";
        }
        String[] parts = categoryStr.split(">");
        return parts[parts.length - 1].trim();
    }
}
