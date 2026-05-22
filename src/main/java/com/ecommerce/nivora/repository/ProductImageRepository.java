package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    void deleteByProductId(Long productId);
}
