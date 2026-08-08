package com.ecommerce.nivora.category.service;

import com.ecommerce.nivora.category.dto.request.CategoryMainRequest;
import com.ecommerce.nivora.category.dto.response.CategoryMainResponse;
import com.ecommerce.nivora.category.entity.CategoryMain;
import com.ecommerce.nivora.category.repository.CategoryMainRepository;
import com.ecommerce.nivora.common.utils.SlugUtils;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
public class CategoryMainServiceImpl implements CategoryMainService {

    private final CategoryMainRepository categoryMainRepository;
    private final SlugUtils slugUtils;

    @Override
    @Cacheable(value = "category_mains", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryMainResponse> getAll() {
        return categoryMainRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryMainResponse> getAllForAdmin() {
        return categoryMainRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryMainResponse getById(Long id) {
        CategoryMain main = categoryMainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryMain", id));
        return toResponse(main);
    }

    @Override
    @CacheEvict(value = "category_mains", allEntries = true)
    @Transactional
    public CategoryMainResponse create(CategoryMainRequest request) {
        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? request.getSlug()
                : slugUtils.uniqueSlug(request.getName(), categoryMainRepository::existsBySlug);

        if (categoryMainRepository.existsBySlug(slug)) {
            throw new ConflictException("CategoryMain slug already exists: " + slug);
        }

        CategoryMain main = CategoryMain.builder()
                .name(request.getName())
                .slug(slug)
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .build();

        categoryMainRepository.save(main);
        return toResponse(main);
    }

    @Override
    @CacheEvict(value = "category_mains", allEntries = true)
    @Transactional
    public CategoryMainResponse update(Long id, CategoryMainRequest request) {
        CategoryMain main = categoryMainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryMain", id));

        main.setName(request.getName());
        main.setImageUrl(request.getImageUrl());
        main.setDisplayOrder(request.getDisplayOrder());

        if (request.getSlug() != null && !request.getSlug().isBlank() && !request.getSlug().equals(main.getSlug())) {
            if (categoryMainRepository.existsBySlug(request.getSlug())) {
                throw new ConflictException("CategoryMain slug already exists: " + request.getSlug());
            }
            main.setSlug(request.getSlug());
        }

        categoryMainRepository.save(main);
        return toResponse(main);
    }

    @Override
    @CacheEvict(value = "category_mains", allEntries = true)
    @Transactional
    public void delete(Long id) {
        CategoryMain main = categoryMainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryMain", id));
        main.setDeletedAt(LocalDateTime.now());
        categoryMainRepository.save(main);
    }

    private CategoryMainResponse toResponse(CategoryMain m) {
        return CategoryMainResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .slug(m.getSlug())
                .imageUrl(m.getImageUrl())
                .displayOrder(m.getDisplayOrder())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
