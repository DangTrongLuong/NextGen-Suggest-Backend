package com.ecommerce.nivora.search.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.search.service.SearchService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Semantic Search proxy endpoints")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Perform search (AI semantic or keyword DB search)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "rerank", defaultValue = "true") boolean rerank,
            @RequestParam(value = "top_k", defaultValue = "10") int topK,
            @RequestParam(value = "use_ai", defaultValue = "true") boolean useAi
    ) {
        List<ProductResponse> results = searchService.search(query, rerank, topK, useAi);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Perform search (AI semantic or keyword DB search) with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchPaginated(
            @RequestParam("q") String query,
            @RequestParam(value = "rerank", defaultValue = "true") boolean rerank,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String categoryMainSlug,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(value = "use_ai", defaultValue = "true") boolean useAi
    ) {
        PageResponse<ProductResponse> results = searchService.searchPaginated(
                query, rerank, page, size, categorySlug, categoryMainSlug, minPrice, maxPrice, minRating, sortBy, sortDir, useAi
        );
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get most popular search keywords")
    public ResponseEntity<ApiResponse<List<String>>> getPopularSearches(
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        List<String> results = searchService.getTopSearchKeywords(limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
