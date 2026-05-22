package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.request.CategoryRequest;
import com.ecommerce.nivora.dto.response.CategoryResponse;
import com.ecommerce.nivora.entity.Category;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.repository.CategoryRepository;
import com.ecommerce.nivora.service.CategoryService;
import com.ecommerce.nivora.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
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
        return categoryRepository.findByIsActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllForAdmin() {
        return categoryRepository.findByDeletedAtIsNullOrderBySortOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        return categoryRepository.findByParentIsNullAndDeletedAtIsNullOrderBySortOrderAsc()
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
                .sortOrder(request.getSortOrder())
                .isActive(request.getIsActive())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .build();

        categoryRepository.save(category);
        return toResponse(category);
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
        category.setSortOrder(request.getSortOrder());
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
                .sortOrder(c.getSortOrder())
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
