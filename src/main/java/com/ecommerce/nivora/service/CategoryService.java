package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.request.CategoryRequest;
import com.ecommerce.nivora.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    List<CategoryResponse> getAllForAdmin();
    List<CategoryResponse> getTree();
    CategoryResponse getById(Long id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
