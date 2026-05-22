package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);
    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);
    void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
    void deleteByProductId(Long productId);
}
