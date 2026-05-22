package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlugAndDeletedAtIsNull(String slug);
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);
    long countByDeletedAtIsNull();

    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
          AND p.isActive = true
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
          AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
          AND (:minRating IS NULL OR p.ratingAvg >= :minRating)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> findWithFilters(
        @Param("categoryId") Long categoryId,
        @Param("brand") String brand,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minRating") BigDecimal minRating,
        @Param("search") String search,
        Pageable pageable
    );

    Page<Product> findByIsFeaturedTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);
    Page<Product> findByIsBestSellerTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);
    Page<Product> findByIsNewTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> findAllForAdmin(
        @Param("categoryId") Long categoryId,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId AND p.isActive = true AND p.deletedAt IS NULL")
    Page<Product> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);
}
