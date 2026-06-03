package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.service.RedisService;
import com.ecommerce.nivora.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Semantic Search proxy endpoints")
public class SearchController {

    private final SearchService searchService;
    private final RedisService redisService;

    @GetMapping
    @Operation(summary = "Perform semantic search & reranking via FastAPI")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "rerank", defaultValue = "false") boolean rerank
    ) {
        List<ProductResponse> results = searchService.search(query, rerank);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/redis-test")
    @Operation(summary = "Test Redis connection")
    public ResponseEntity<ApiResponse<String>> testRedis() {
        try {
            String testKey = "test:connection:api";
            String testValue = "Spring Boot 3 + Redis Connection Successful!";
            
            redisService.set(testKey, testValue, 1, TimeUnit.MINUTES);
            String retrievedValue = (String) redisService.get(testKey);
            redisService.delete(testKey);
            
            return ResponseEntity.ok(ApiResponse.success(retrievedValue));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Redis Connection Failed: " + e.getMessage()));
        }
    }
}
