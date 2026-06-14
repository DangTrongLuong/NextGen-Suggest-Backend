package com.ecommerce.nivora.product.repository;

import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.entity.ProductVariant;


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
    Optional<Product> findByUuidAndDeletedAtIsNull(String uuid);

    @Query("SELECT p FROM Product p JOIN p.variants v WHERE v.sku = :sku AND p.deletedAt IS NULL AND v.deletedAt IS NULL")
    Optional<Product> findBySkuAndDeletedAtIsNull(@Param("sku") String sku);

    boolean existsBySlug(String slug);

    Optional<Product> findByProductUrlAndDeletedAtIsNull(String productUrl);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND LOWER(p.brand) = LOWER(:brand) AND p.deletedAt IS NULL")
    java.util.List<Product> findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull(
            @Param("name") String name, @Param("brand") String brand);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.brand) = LOWER(:brand) AND p.deletedAt IS NULL")
    boolean existsByBrandIgnoreCaseAndDeletedAtIsNull(@Param("brand") String brand);

    @Query("SELECT COUNT(v) > 0 FROM ProductVariant v WHERE v.sku = :sku AND v.deletedAt IS NULL")
    boolean existsBySku(@Param("sku") String sku);

    long countByDeletedAtIsNull();

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE v.sku IN :skus AND p.deletedAt IS NULL AND v.deletedAt IS NULL AND p.isActive = true AND v.isActive = true")
    java.util.List<Product> findBySkuInAndDeletedAtIsNullAndIsActiveTrue(@Param("skus") java.util.List<String> skus);

    @Query("SELECT p FROM Product p WHERE p.uuid IN :uuids AND p.isActive = true AND p.deletedAt IS NULL")
    java.util.List<Product> findByUuidInAndIsActiveTrueAndDeletedAtIsNull(@Param("uuids") java.util.List<String> uuids);

    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
          AND p.isActive = true
          AND (:applyCategoryFilter = false OR p.category.id IN :categoryIds)
          AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
          AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
          AND (:minRating IS NULL OR p.ratingAvg >= :minRating)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> findWithFilters(
        @Param("applyCategoryFilter") boolean applyCategoryFilter,
        @Param("categoryIds") java.util.List<Long> categoryIds,
        @Param("brand") String brand,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minRating") BigDecimal minRating,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.isActive = true ORDER BY p.isFeatured DESC, p.ratingAvg DESC, p.views DESC")
    Page<Product> findByIsFeaturedTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.isActive = true ORDER BY p.isBestSeller DESC, (p.ratingCount * 5 + p.views) DESC")
    Page<Product> findByIsBestSellerTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.isActive = true ORDER BY p.isNew DESC, p.createdAt DESC")
    Page<Product> findByIsNewTrueAndIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.isActive = true ORDER BY p.views DESC, p.ratingCount DESC")
    Page<Product> findTrendingFallback(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))
               OR EXISTS (SELECT 1 FROM ProductVariant v WHERE v.product = p AND v.deletedAt IS NULL AND LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%'))))
        """)
    Page<Product> findAllForAdmin(
        @Param("categoryId") Long categoryId,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId AND p.isActive = true AND p.deletedAt IS NULL")
    Page<Product> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);

    @Query("SELECT p.category.name, COUNT(p) FROM Product p WHERE p.deletedAt IS NULL GROUP BY p.category.name")
    java.util.List<Object[]> getProductCountByCategory();
}
