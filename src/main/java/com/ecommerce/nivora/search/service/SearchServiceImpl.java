package com.ecommerce.nivora.search.service;

import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.dto.response.ProductImageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.dto.response.ProductVariantResponse;
import com.ecommerce.nivora.product.service.ProductService;
import com.ecommerce.nivora.recommendation.FastApiProductResult;
import com.ecommerce.nivora.search.dto.response.FastApiSearchResponse;
import com.ecommerce.nivora.search.entity.SearchLog;
import com.ecommerce.nivora.search.repository.SearchLogRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.category.entity.CategoryMain;
import com.ecommerce.nivora.category.repository.CategoryMainRepository;
import com.ecommerce.nivora.search.util.SearchQuerySanitizer;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;


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

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final RestTemplate restTemplate;
    private final ProductService productService;
    private final SearchLogRepository searchLogRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMainRepository categoryMainRepository;
    private final StringRedisTemplate redisTemplate;

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-z0-9]+");

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public List<ProductResponse> search(String query, boolean rerank, int topK) {
        if (query == null || query.trim().isEmpty() || SearchQuerySanitizer.isMaliciousOrInvalid(query)) {
            if (query != null && SearchQuerySanitizer.isMaliciousOrInvalid(query)) {
                log.warn("Blocked malicious/invalid search query: '{}'", query);
            }
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        int safeTopK = Math.min(Math.max(topK, 1), 150);
        int candidateK = rerank ? Math.min(Math.max((int) (safeTopK * 1.5), 15), 200) : safeTopK;

        try {
            java.net.URI targetUri = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/search")
                    .queryParam("q", query)
                    .queryParam("use_reranking", rerank)
                    .queryParam("candidate_k", candidateK)
                    .queryParam("top_k", safeTopK)
                    .build()
                    .encode(java.nio.charset.StandardCharsets.UTF_8)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                if (attr != null && attr.getRequest() != null) {
                    String sessionIdHeader = attr.getRequest().getHeader("X-Session-Id");
                    if (sessionIdHeader != null) {
                        headers.set("X-Session-Id", sessionIdHeader);
                    }
                    String userIdHeader = attr.getRequest().getHeader("X-User-Id");
                    if (userIdHeader != null) {
                        headers.set("X-User-Id", userIdHeader);
                    } else {
                        try {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                                userRepository.findActiveByEmail(auth.getName()).ifPresent(u -> headers.set("X-User-Id", String.valueOf(u.getId())));
                            }
                        } catch (Exception ignored) {}
                    }
                    String clientStartHeader = attr.getRequest().getHeader("X-Client-Start-Time");
                    if (clientStartHeader != null) {
                        headers.set("X-Client-Start-Time", clientStartHeader);
                    }
                }
            } catch (Exception e) {
                // Ignore context failures (e.g. background threads)
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<FastApiSearchResponse> responseEntity = restTemplate.exchange(targetUri, HttpMethod.GET, requestEntity, FastApiSearchResponse.class);
            FastApiSearchResponse apiResponse = responseEntity.getBody();

            if (apiResponse == null || apiResponse.getResults() == null || apiResponse.getResults().isEmpty()) {
                log.info("No semantic search results returned from FastAPI for query: {}", query);
                return Collections.emptyList();
            }

            List<String> uuids = apiResponse.getResults().stream()
                    .map(FastApiProductResult::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, ProductResponse> dbProductsByUuid = productService.getActiveProductsByUuids(uuids);
            log.info("Hydrated {}/{} semantic results from database. Requested UUIDs: {}, Found Keys: {}", dbProductsByUuid.size(), uuids.size(), uuids, dbProductsByUuid.keySet());

            String reqId = apiResponse.getRequestId();
            List<ProductResponse> results = apiResponse.getResults().stream()
                    .map(res -> {
                        ProductResponse p = dbProductsByUuid.get(res.getProductId());
                        if (p == null) {
                            p = mapFromFastApi(res);
                        }
                        if (p != null && reqId != null) {
                            p.setRequestId(reqId);
                        }
                        return p;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                throw new RuntimeException("No active matching products found in local database for the returned semantic UUIDs");
            }

            return results;

        } catch (Exception e) {
            log.error("Failed to perform semantic search via FastAPI, falling back to database search", e);
            logSearch(query, 0, (int) (System.currentTimeMillis() - startTime));
            try {
                PageResponse<ProductResponse> dbResult = productService.getAll(
                        null, null, null, null, null, null, null, query, PageRequest.of(0, safeTopK)
                );
                return dbResult.getContent();
            } catch (Exception ex) {
                log.error("Database fallback search failed", ex);
                return Collections.emptyList();
            }
        }
    }

    @Override
    public PageResponse<ProductResponse> searchPaginated(
            String query, boolean rerank, int page, int size,
            String categorySlug, String categoryMainSlug,
            Double minPrice, Double maxPrice, Double minRating,
            String sortBy, String sortDir
    ) {
        if (query == null || query.trim().isEmpty() || SearchQuerySanitizer.isMaliciousOrInvalid(query)) {
            if (query != null && SearchQuerySanitizer.isMaliciousOrInvalid(query)) {
                log.warn("Blocked malicious/invalid paginated search query: '{}'", query);
            }
            return PageResponse.<ProductResponse>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        long startTime = System.currentTimeMillis();
        try {
            // Resolve Category Name if categorySlug is present
            String categoryName = null;
            if (categorySlug != null && !categorySlug.trim().isEmpty()) {
                categoryName = categoryRepository.findBySlugAndDeletedAtIsNull(categorySlug)
                        .map(Category::getName)
                        .orElse(null);
            }

            // Resolve CategoryMain ID if categoryMainSlug is present
            Long mainId = null;
            if (categoryMainSlug != null && !categoryMainSlug.trim().isEmpty()) {
                mainId = categoryMainRepository.findBySlugAndDeletedAtIsNull(categoryMainSlug)
                        .map(CategoryMain::getId)
                        .orElse(null);
            }

            // Determine if we need to post-process filter/sort in Java
            boolean needsJavaFilterOrSort = (minRating != null)
                    || (mainId != null)
                    || (sortBy != null && !sortBy.trim().isEmpty() && !sortBy.equalsIgnoreCase("newest") && !sortBy.equalsIgnoreCase("createdAt"));

            // If we need post-processing, fetch a smaller reasonable pool (max 25) from FastAPI
            int fetchSize = needsJavaFilterOrSort ? 25 : size;
            int fetchPage = needsJavaFilterOrSort ? 0 : page;

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fastApiUrl)
                    .path("/api/v1/search")
                    .queryParam("q", query)
                    .queryParam("use_reranking", rerank)
                    .queryParam("page", fetchPage)
                    .queryParam("size", fetchSize);

            if (categoryName != null) {
                builder.queryParam("category", categoryName);
            }
            if (minPrice != null) {
                builder.queryParam("min_price", minPrice);
            }
            if (maxPrice != null) {
                builder.queryParam("max_price", maxPrice);
            }

            java.net.URI targetUri = builder.build()
                    .encode(java.nio.charset.StandardCharsets.UTF_8)
                    .toUri();

            log.info("Forwarding paginated search query '{}' to FastAPI: {}", query, targetUri);

            HttpHeaders headers = new HttpHeaders();
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                if (attr != null && attr.getRequest() != null) {
                    String sessionIdHeader = attr.getRequest().getHeader("X-Session-Id");
                    if (sessionIdHeader != null) {
                        headers.set("X-Session-Id", sessionIdHeader);
                    }
                    String userIdHeader = attr.getRequest().getHeader("X-User-Id");
                    if (userIdHeader != null) {
                        headers.set("X-User-Id", userIdHeader);
                    }
                    String clientStartHeader = attr.getRequest().getHeader("X-Client-Start-Time");
                    if (clientStartHeader != null) {
                        headers.set("X-Client-Start-Time", clientStartHeader);
                    }
                }
            } catch (Exception e) {
                // Ignore context failures
            }
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<FastApiSearchResponse> responseEntity = restTemplate.exchange(targetUri, HttpMethod.GET, requestEntity, FastApiSearchResponse.class);
            FastApiSearchResponse apiResponse = responseEntity.getBody();

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

            List<FastApiProductResult> resultsList = apiResponse.getResults();
            List<String> uuids = resultsList.stream()
                    .map(FastApiProductResult::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, ProductResponse> dbProductsByUuid = productService.getActiveProductsByUuids(uuids);
            log.info("Hydrated {}/{} semantic results from database. Requested UUIDs: {}, Found Keys: {}", dbProductsByUuid.size(), uuids.size(), uuids, dbProductsByUuid.keySet());

            String reqId = apiResponse.getRequestId();
            List<ProductResponse> hydratedList = resultsList.stream()
                    .map(res -> {
                        ProductResponse p = dbProductsByUuid.get(res.getProductId());
                        if (p == null) {
                            p = mapFromFastApi(res);
                        }
                        if (p != null && reqId != null) {
                            p.setRequestId(reqId);
                        }
                        return p;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (hydratedList.isEmpty()) {
                throw new RuntimeException("No active matching products found in local database for the returned semantic UUIDs");
            }

            if (needsJavaFilterOrSort) {
                List<ProductResponse> filteredList = hydratedList;

                // 1. Filter by CategoryMain ID
                if (mainId != null) {
                    final Long finalMainId = mainId;
                    filteredList = filteredList.stream()
                            .filter(p -> p.getCategory() != null && finalMainId.equals(p.getCategory().getCategoryMainId()))
                            .collect(Collectors.toList());
                }

                // 2. Filter by minRating
                if (minRating != null) {
                    filteredList = filteredList.stream()
                            .filter(p -> p.getRatingAvg() != null && p.getRatingAvg().doubleValue() >= minRating)
                            .collect(Collectors.toList());
                }

                // 3. Sort by price or date
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    if (sortBy.equalsIgnoreCase("price-asc")) {
                        filteredList.sort(Comparator.comparing(ProductResponse::getBasePrice, Comparator.nullsLast(Comparator.naturalOrder())));
                    } else if (sortBy.equalsIgnoreCase("price-desc")) {
                        filteredList.sort(Comparator.comparing(ProductResponse::getBasePrice, Comparator.nullsLast(Comparator.reverseOrder())));
                    } else if (sortBy.equalsIgnoreCase("newest")) {
                        filteredList.sort(Comparator.comparing(ProductResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                    }
                }

                int totalElements = filteredList.size();
                int totalPages = (int) Math.ceil((double) totalElements / size);

                int fromIndex = page * size;
                int toIndex = Math.min(totalElements, (page + 1) * size);

                List<ProductResponse> content = Collections.emptyList();
                if (fromIndex < totalElements) {
                    content = filteredList.subList(fromIndex, toIndex);
                }

                return PageResponse.<ProductResponse>builder()
                        .content(content)
                        .page(page)
                        .size(size)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .last(page >= totalPages - 1)
                        .build();
            } else {
                // Return standard Qdrant-paginated items
                int actualSize = hydratedList.size();
                int totalElements;
                if (actualSize >= size) {
                    totalElements = (page + 2) * size;
                } else {
                    totalElements = page * size + actualSize;
                }
                int totalPages = (int) Math.ceil((double) totalElements / size);

                return PageResponse.<ProductResponse>builder()
                        .content(hydratedList)
                        .page(page)
                        .size(size)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .last(page >= totalPages - 1)
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to perform paginated semantic search via FastAPI, falling back to database search", e);
            logSearch(query, 0, (int) (System.currentTimeMillis() - startTime));
            try {
                org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
                if (sortBy != null && !sortBy.trim().isEmpty()) {
                    if (sortBy.equalsIgnoreCase("price-asc")) {
                        sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "basePrice");
                    } else if (sortBy.equalsIgnoreCase("price-desc")) {
                        sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "basePrice");
                    } else if (sortBy.equalsIgnoreCase("newest")) {
                        sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
                    }
                }
                PageRequest pageRequest = PageRequest.of(page, size, sort);

                return productService.getAll(
                        null,
                        categorySlug,
                        categoryMainSlug,
                        null,
                        minPrice != null ? BigDecimal.valueOf(minPrice) : null,
                        maxPrice != null ? BigDecimal.valueOf(maxPrice) : null,
                        minRating != null ? BigDecimal.valueOf(minRating) : null,
                        query,
                        pageRequest
                );
            } catch (Exception ex) {
                log.error("Database fallback paginated search failed", ex);
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
    }

    @Override
    public List<String> getTopSearchKeywords(int limit) {
        try {
            int safeLimit = Math.max(1, limit);
            List<Object[]> topQueries = searchLogRepository.findTopQueries(PageRequest.of(0, safeLimit));
            if (topQueries == null) {
                return Collections.emptyList();
            }
            return topQueries.stream()
                    .map(row -> row != null && row.length > 0 && row[0] != null ? row[0].toString().trim() : null)
                    .filter(q -> q != null && !q.isEmpty() && !SearchQuerySanitizer.isMaliciousOrInvalid(q))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch top search keywords", e);
            return Collections.emptyList();
        }
    }

    private void logSearch(String query, int resultsCount, int latencyMs) {
        if (SearchQuerySanitizer.isMaliciousOrInvalid(query)) {
            return;
        }
        try {
            String sessionId = "anonymous";
            int finalLatencyMs = latencyMs;
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                if (attr != null && attr.getRequest() != null) {
                    sessionId = attr.getRequest().getHeader("X-Session-Id");
                    if (sessionId == null || sessionId.isBlank()) {
                        if (attr.getRequest().getSession(false) != null) {
                            sessionId = attr.getRequest().getSession(false).getId();
                        } else {
                            sessionId = attr.getRequest().getRemoteAddr();
                        }
                    }
                    String clientStartHeader = attr.getRequest().getHeader("X-Client-Start-Time");
                    if (clientStartHeader != null) {
                        try {
                            long clientStart = Long.parseLong(clientStartHeader);
                            long now = System.currentTimeMillis();
                            if (now > clientStart) {
                                finalLatencyMs = (int) (now - clientStart);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                // Context attributes not available (e.g. background task)
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = null;
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                user = userRepository.findActiveByEmail(auth.getName()).orElse(null);
            }

            SearchLog searchLog = SearchLog.builder()
                    .sessionId(sessionId != null && sessionId.length() > 255 ? sessionId.substring(0, 255) : sessionId)
                    .user(user)
                    .queryText(query != null && query.length() > 500 ? query.substring(0, 500) : query)
                    .resultsCount(resultsCount)
                    .latencyMs(finalLatencyMs)
                    .build();

            searchLogRepository.save(searchLog);
        } catch (Exception e) {
            log.error("Failed to log search query to MySQL", e);
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
