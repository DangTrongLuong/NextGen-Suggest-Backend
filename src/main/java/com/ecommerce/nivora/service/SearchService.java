package com.ecommerce.nivora.service;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.ProductResponse;
import java.util.List;

public interface SearchService {
    List<ProductResponse> search(String query, boolean rerank, int topK);
    PageResponse<ProductResponse> searchPaginated(String query, boolean rerank, int page, int size);
}
