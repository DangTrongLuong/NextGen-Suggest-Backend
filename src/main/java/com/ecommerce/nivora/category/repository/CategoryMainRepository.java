package com.ecommerce.nivora.category.repository;

import com.ecommerce.nivora.category.entity.CategoryMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryMainRepository extends JpaRepository<CategoryMain, Long> {
    Optional<CategoryMain> findBySlugAndDeletedAtIsNull(String slug);
    Optional<CategoryMain> findByNameAndDeletedAtIsNull(String name);
    List<CategoryMain> findByDeletedAtIsNullOrderByDisplayOrderAsc();
    boolean existsBySlug(String slug);
}
