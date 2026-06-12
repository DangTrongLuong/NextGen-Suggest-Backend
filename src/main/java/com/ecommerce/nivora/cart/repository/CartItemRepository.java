package com.ecommerce.nivora.cart.repository;

import com.ecommerce.nivora.cart.entity.Cart;
import com.ecommerce.nivora.cart.entity.CartItem;
import com.ecommerce.nivora.product.entity.ProductVariant;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
    void deleteByCartId(Long cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.variant.product.id = :productId")
    void deleteByProductId(Long productId);
}
