package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugAndDeletedAtIsNull(String slug);
    Optional<Category> findByNameAndDeletedAtIsNull(String name);
    List<Category> findByParentIsNullAndDeletedAtIsNullOrderByDisplayOrderAsc();
    List<Category> findByParentIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long parentId);
    List<Category> findByIsActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc();
    List<Category> findByDeletedAtIsNullOrderByDisplayOrderAsc();
    boolean existsBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.deletedAt IS NULL " +
            "AND (:parentOnly = false OR c.parent IS NULL) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.displayOrder ASC")
    Page<Category> findActiveCategories(
            @Param("search") String search,
            @Param("parentOnly") boolean parentOnly,
            Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.deletedAt IS NULL " +
            "AND (:parentOnly = false OR c.parent IS NULL) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findActiveCategories(
            @Param("search") String search,
            @Param("parentOnly") boolean parentOnly);
}
