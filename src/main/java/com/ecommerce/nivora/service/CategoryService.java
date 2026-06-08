package com.ecommerce.nivora.service;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.request.CategoryRequest;
import com.ecommerce.nivora.dto.response.CategoryResponse;
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
