package com.ecommerce.nivora.order.repository;

import com.ecommerce.nivora.order.entity.Order;
import com.ecommerce.nivora.order.entity.OrderItem;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByOrderNumberStartingWith(String prefix);

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

    @Query("SELECT o.createdAt, o.totalAmount, o.status FROM Order o WHERE o.createdAt >= :startDate")
    List<Object[]> getOrderSummaryForCharts(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT oi.productName, SUM(oi.quantity) as soldCount, SUM(oi.totalPrice) as revenue " +
           "FROM OrderItem oi " +
           "GROUP BY oi.productName " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopSellingProducts(Pageable pageable);

    @Query("SELECT DISTINCT oi.product.uuid FROM Order o JOIN o.items oi WHERE o.user.id = :userId AND o.status <> 'CANCELLED'")
    List<String> findPurchasedProductUuidsByUserId(@Param("userId") Long userId);

    @Query("SELECT oi.product.uuid, COUNT(oi.id) as count " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN o.shippingAddress sa " +
           "WHERE sa.city LIKE CONCAT('%', :city, '%') OR sa.state LIKE CONCAT('%', :city, '%') " +
           "GROUP BY oi.product.uuid " +
           "ORDER BY count DESC")
    List<String> findTopProductsByLocation(@Param("city") String city, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT oi.product.id, COUNT(oi.id) as count " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN o.shippingAddress sa " +
           "WHERE sa.ward IS NOT NULL AND LOWER(sa.ward) = LOWER(:ward) AND o.status <> 'CANCELLED' " +
           "GROUP BY oi.product.id " +
           "ORDER BY count DESC")
    List<Object[]> findTopProductsByWard(@Param("ward") String ward, Pageable pageable);

    @Query("SELECT oi.product.id, COUNT(oi.id) as count " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN o.shippingAddress sa " +
           "WHERE sa.district IS NOT NULL AND LOWER(sa.district) = LOWER(:district) AND o.status <> 'CANCELLED' " +
           "GROUP BY oi.product.id " +
           "ORDER BY count DESC")
    List<Object[]> findTopProductsByDistrict(@Param("district") String district, Pageable pageable);

    @Query("SELECT oi.product.id, COUNT(oi.id) as count " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN o.shippingAddress sa " +
           "WHERE sa.province IS NOT NULL AND LOWER(sa.province) = LOWER(:province) AND o.status <> 'CANCELLED' " +
           "GROUP BY oi.product.id " +
           "ORDER BY count DESC")
    List<Object[]> findTopProductsByProvince(@Param("province") String province, Pageable pageable);
}
