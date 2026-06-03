package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.ProductResponse;
import java.util.List;

public interface RedisCacheService {
    
    /**
     * Retrieves cached search results for the given query and rerank flag.
     *
     * @param query  the search query keyword
     * @param rerank the reranking flag
     * @return the list of cached ProductResponses, or null if cache miss or error
     */
    List<ProductResponse> getCachedSearchResults(String query, boolean rerank);

    /**
     * Caches search results for the given query and rerank flag.
     *
     * @param query   the search query keyword
     * @param rerank  the reranking flag
     * @param results the search results to cache
     */
    void cacheSearchResults(String query, boolean rerank, List<ProductResponse> results);
}
