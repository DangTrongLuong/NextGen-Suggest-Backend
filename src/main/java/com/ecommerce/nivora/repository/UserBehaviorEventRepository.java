package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.UserBehaviorEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserBehaviorEventRepository extends JpaRepository<UserBehaviorEvent, Long> {

    @Query("SELECT e.product.uuid FROM UserBehaviorEvent e WHERE e.user.id = :userId AND e.eventType = 'product_view' AND e.product IS NOT NULL GROUP BY e.product.uuid ORDER BY MAX(e.createdAt) DESC")
    List<String> findRecentViewedProductUuidsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e.product.uuid FROM UserBehaviorEvent e WHERE e.sessionId = :sessionId AND e.eventType = 'product_view' AND e.product IS NOT NULL GROUP BY e.product.uuid ORDER BY MAX(e.createdAt) DESC")
    List<String> findRecentViewedProductUuidsBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    long countByProductIdAndEventType(Long productId, String eventType);

    @Query("SELECT e.product.id, COUNT(e) FROM UserBehaviorEvent e WHERE e.eventType = :eventType GROUP BY e.product.id")
    List<Object[]> countEventsGroupByProductId(@Param("eventType") String eventType);

    @Query(value = "SELECT product_id, " +
            " (0.5 * SUM(CASE WHEN event_type = 'product_view' THEN 1 ELSE 0 END) + " +
            "  0.3 * SUM(CASE WHEN event_type = 'purchase' THEN 1 ELSE 0 END) + " +
            "  0.2 * SUM(CASE WHEN event_type = 'wishlist' THEN 1 ELSE 0 END)) as score " +
            " FROM user_behavior_events " +
            " WHERE created_at >= :sinceDate AND product_id IS NOT NULL " +
            " GROUP BY product_id " +
            " ORDER BY score DESC", nativeQuery = true)
    List<Object[]> findTrendingProductsSince(@Param("sinceDate") java.time.LocalDateTime sinceDate, Pageable pageable);
}
