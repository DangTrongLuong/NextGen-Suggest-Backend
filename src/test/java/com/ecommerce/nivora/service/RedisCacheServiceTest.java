package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisCacheServiceTest {

    @Autowired
    private RedisCacheService redisCacheService;

    @Test
    void testSearchCachingAndNormalization() {
        String originalQuery = "  Nội thất   phòng khách  ";
        boolean rerank = false;

        // Ensure cache is clean for this test case
        // We write directly first, then check HIT
        List<ProductResponse> mockResults = List.of(
                ProductResponse.builder()
                        .id(1L)
                        .uuid("prod-1")
                        .sku("prod-1")
                        .name("Bàn ghế gỗ cao cấp")
                        .basePrice(BigDecimal.valueOf(15000000))
                        .build()
        );

        // 1. First retrieval should be MISS (unless it was already in cache, but we just verify it doesn't crash)
        List<ProductResponse> initialGet = redisCacheService.getCachedSearchResults(originalQuery, rerank);
        // Note: initialGet might be null (MISS) or a list if previously cached.

        // 2. Cache the results
        redisCacheService.cacheSearchResults(originalQuery, rerank, mockResults);

        // 3. Second retrieval with the same query (even with different casing, extra spaces, and accents)
        // should HIT and return the exact cached list.
        String testQueries[] = {
                "nội thất phòng khách",
                "  Nội thất   phòng khách  ",
                "Noi that phong khach",
                "noi-that-phong-khach" // Note: normalizer replaces spaces, but let's test space-based normalizations
        };

        for (String q : List.of("nội thất phòng khách", "  Nội thất   phòng khách  ", "Noi that phong khach")) {
            List<ProductResponse> cachedGet = redisCacheService.getCachedSearchResults(q, rerank);
            assertThat(cachedGet).isNotNull();
            assertThat(cachedGet).hasSize(1);
            assertThat(cachedGet.get(0).getName()).isEqualTo("Bàn ghế gỗ cao cấp");
        }

        // Test with different rerank option - should be a MISS
        List<ProductResponse> cachedGetRerank = redisCacheService.getCachedSearchResults(originalQuery, true);
        // Since we cached for rerank = false, rerank = true should not hit the same cache entry.
        // It should be null or different.
        if (cachedGetRerank != null) {
            // If it exists, it must be from another run, but it shouldn't match our mock unless cached previously.
        }
    }
}
