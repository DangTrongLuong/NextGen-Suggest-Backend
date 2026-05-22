package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Cart;
import com.ecommerce.nivora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findByUserId(Long userId);
    Optional<Cart> findBySessionId(String sessionId);
}
