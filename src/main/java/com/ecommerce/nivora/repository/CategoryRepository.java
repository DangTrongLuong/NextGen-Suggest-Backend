package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugAndDeletedAtIsNull(String slug);
    Optional<Category> findByNameAndDeletedAtIsNull(String name);
    List<Category> findByParentIsNullAndDeletedAtIsNullOrderBySortOrderAsc();
    List<Category> findByParentIdAndDeletedAtIsNullOrderBySortOrderAsc(Long parentId);
    List<Category> findByIsActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();
    List<Category> findByDeletedAtIsNullOrderBySortOrderAsc();
    boolean existsBySlug(String slug);
}
