package com.ecommerce.nivora.product.repository;

import com.ecommerce.nivora.product.entity.ProductVariant;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndDeletedAtIsNullOrderBySortOrderAsc(Long productId);
    Optional<ProductVariant> findBySkuAndDeletedAtIsNull(String sku);
    boolean existsBySku(String sku);
}
