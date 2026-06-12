package com.ecommerce.nivora.category.service;

import com.ecommerce.nivora.category.dto.request.CategoryRequest;
import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.common.utils.SlugUtils;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugUtils slugUtils;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findByIsActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(String search, Boolean parentOnly) {
        boolean onlyParent = parentOnly != null && parentOnly;
        return categoryRepository.findActiveCategories(search, onlyParent)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getPaginated(String search, Boolean parentOnly, int page, int size) {
        boolean onlyParent = parentOnly != null && parentOnly;
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryRepository.findActiveCategories(search, onlyParent, pageable);
        return PageResponse.of(categoryPage.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllForAdmin() {
        return categoryRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        return categoryRepository.findByParentIsNullAndDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toResponseWithChildren).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return toResponseWithChildren(cat);
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? request.getSlug()
                : slugUtils.uniqueSlug(request.getName(), categoryRepository::existsBySlug);

        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Category slug already exists: " + slug);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .icon(request.getIcon())
                .parent(parent)
                .displayOrder(request.getSortOrder())
                .isActive(request.getIsActive())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .build();

        categoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getBreadcrumb(String slug) {
        Category category = categoryRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
        List<Category> path = new java.util.ArrayList<>();
        Category current = category;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setIcon(request.getIcon());
        category.setDisplayOrder(request.getSortOrder());
        category.setIsActive(request.getIsActive());
        category.setSeoTitle(request.getSeoTitle());
        category.setSeoDescription(request.getSeoDescription());

        if (request.getParentId() != null && !request.getParentId().equals(id)) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.getParentId()));
            category.setParent(parent);
        } else if (request.getParentId() == null) {
            category.setParent(null);
        }

        categoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .icon(c.getIcon())
                .sortOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .productCount(c.getProducts() != null ? c.getProducts().size() : 0)
                .build();
    }

    private CategoryResponse toResponseWithChildren(Category c) {
        CategoryResponse response = toResponse(c);
        if (c.getChildren() != null && !c.getChildren().isEmpty()) {
            response.setChildren(c.getChildren().stream()
                    .filter(child -> child.getDeletedAt() == null)
                    .map(this::toResponseWithChildren)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
