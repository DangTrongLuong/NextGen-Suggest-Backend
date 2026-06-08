package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.*;
import com.ecommerce.nivora.service.ProductService;
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
    private final ProductService productService;

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-z0-9]+");

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public List<ProductResponse> search(String query, boolean rerank, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int safeTopK = Math.min(Math.max(topK, 1), 150);
        int candidateK = rerank ? Math.min(Math.max((int) (safeTopK * 1.5), 15), 200) : safeTopK;

        try {
            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/search")
                    .queryParam("q", query)
                    .queryParam("use_reranking", rerank)
                    .queryParam("candidate_k", candidateK)
                    .queryParam("top_k", safeTopK)
                    .build()
                    .toUriString();

            log.info("Forwarding search query '{}' (rerank={}, topK={}) to FastAPI: {}", query, rerank, safeTopK, targetUrl);

            FastApiSearchResponse apiResponse = restTemplate.getForObject(targetUrl, FastApiSearchResponse.class);

            if (apiResponse == null || apiResponse.getResults() == null || apiResponse.getResults().isEmpty()) {
                log.info("No semantic search results returned from FastAPI for query: {}", query);
                return Collections.emptyList();
            }

            List<String> skus = apiResponse.getResults().stream()
                    .map(FastApiProductResult::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, ProductResponse> dbProductsBySku = productService.getActiveProductsBySkus(skus);
            log.info("Hydrated {}/{} semantic results from database", dbProductsBySku.size(), skus.size());

            return apiResponse.getResults().stream()
                    .map(res -> {
                        ProductResponse dbProduct = dbProductsBySku.get(res.getProductId());
                        return dbProduct != null ? dbProduct : mapFromFastApi(res);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to perform semantic search via FastAPI", e);
            return Collections.emptyList();
        }
    }

    @Override
    public PageResponse<ProductResponse> searchPaginated(String query, boolean rerank, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            return PageResponse.<ProductResponse>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        int fetchTopK = 120;
        int candidateK = rerank ? (int) (fetchTopK * 1.5) : fetchTopK;

        try {
            String targetUrl = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/search")
                    .queryParam("q", query)
                    .queryParam("use_reranking", rerank)
                    .queryParam("candidate_k", candidateK)
                    .queryParam("top_k", fetchTopK)
                    .build()
                    .toUriString();

            log.info("Forwarding paginated search query '{}' (rerank={}, page={}, size={}) to FastAPI: {}", query, rerank, page, size, targetUrl);

            FastApiSearchResponse apiResponse = restTemplate.getForObject(targetUrl, FastApiSearchResponse.class);

            if (apiResponse == null || apiResponse.getResults() == null || apiResponse.getResults().isEmpty()) {
                log.info("No semantic search results returned from FastAPI for paginated query: {}", query);
                return PageResponse.<ProductResponse>builder()
                        .content(Collections.emptyList())
                        .page(page)
                        .size(size)
                        .totalElements(0)
                        .totalPages(0)
                        .last(true)
                        .build();
            }

            List<FastApiProductResult> allResults = apiResponse.getResults();
            int totalElements = allResults.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);

            int fromIndex = page * size;
            if (fromIndex >= totalElements) {
                return PageResponse.<ProductResponse>builder()
                        .content(Collections.emptyList())
                        .page(page)
                        .size(size)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .last(true)
                        .build();
            }

            int toIndex = Math.min(fromIndex + size, totalElements);
            List<FastApiProductResult> pageResults = allResults.subList(fromIndex, toIndex);

            List<String> skus = pageResults.stream()
                    .map(FastApiProductResult::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, ProductResponse> dbProductsBySku = productService.getActiveProductsBySkus(skus);
            log.info("Hydrated {}/{} semantic results for page {} from database", dbProductsBySku.size(), skus.size(), page);

            List<ProductResponse> content = pageResults.stream()
                    .map(res -> {
                        ProductResponse dbProduct = dbProductsBySku.get(res.getProductId());
                        return dbProduct != null ? dbProduct : mapFromFastApi(res);
                    })
                    .collect(Collectors.toList());

            return PageResponse.<ProductResponse>builder()
                    .content(content)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .last(page >= totalPages - 1)
                    .build();

        } catch (Exception e) {
            log.error("Failed to perform semantic search via FastAPI", e);
            return PageResponse.<ProductResponse>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }
    }

    private ProductResponse mapFromFastApi(FastApiProductResult res) {
        long fakeId = Math.abs((long) res.getProductId().hashCode());
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
    }

    private String getLeafCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return "Other";
        }
        String[] parts = categoryStr.split(">");
        return parts[parts.length - 1].trim();
    }
}
