package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.service.SearchService;
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
    @Operation(summary = "Perform semantic search & reranking via FastAPI")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "rerank", defaultValue = "true") boolean rerank,
            @RequestParam(value = "top_k", defaultValue = "10") int topK
    ) {
        List<ProductResponse> results = searchService.search(query, rerank, topK);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Perform semantic search & reranking via FastAPI with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchPaginated(
            @RequestParam("q") String query,
            @RequestParam(value = "rerank", defaultValue = "true") boolean rerank,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageResponse<ProductResponse> results = searchService.searchPaginated(query, rerank, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
