package com.ecommerce.nivora.search.service;

import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;


import java.util.List;

public interface SearchService {
    List<ProductResponse> search(String query, boolean rerank, int topK);
    PageResponse<ProductResponse> searchPaginated(String query, boolean rerank, int page, int size);
    List<String> getTopSearchKeywords(int limit);
}
