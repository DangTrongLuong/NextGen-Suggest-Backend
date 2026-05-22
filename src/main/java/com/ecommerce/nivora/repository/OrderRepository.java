package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.Status status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.Status status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status <> 'CANCELLED'")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.status = 'DELIVERED'")
    BigDecimal sumTotalSpentByUserId(@Param("userId") Long userId);
}
