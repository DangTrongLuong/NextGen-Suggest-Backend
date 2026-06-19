package com.ecommerce.nivora.category.service;

import com.ecommerce.nivora.category.dto.request.CategoryMainRequest;
import com.ecommerce.nivora.category.dto.response.CategoryMainResponse;
import java.util.List;

public interface CategoryMainService {
    List<CategoryMainResponse> getAll();
    List<CategoryMainResponse> getAllForAdmin();
    CategoryMainResponse getById(Long id);
    CategoryMainResponse create(CategoryMainRequest request);
    CategoryMainResponse update(Long id, CategoryMainRequest request);
    void delete(Long id);
}
