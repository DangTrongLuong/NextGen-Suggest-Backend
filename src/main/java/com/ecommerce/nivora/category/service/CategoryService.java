package com.ecommerce.nivora.category.service;

import com.ecommerce.nivora.category.dto.request.CategoryRequest;
import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;


import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    List<CategoryResponse> getAll(String search, Boolean parentOnly);
    PageResponse<CategoryResponse> getPaginated(String search, Boolean parentOnly, int page, int size);
    List<CategoryResponse> getAllForAdmin();
    List<CategoryResponse> getTree();
    List<CategoryResponse> getBreadcrumb(String slug);
    CategoryResponse getById(Long id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
