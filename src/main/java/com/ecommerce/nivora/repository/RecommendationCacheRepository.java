package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.RecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {

    Optional<RecommendationCache> findByUserIdAndRecType(Long userId, String recType);

    Optional<RecommendationCache> findBySessionIdAndRecType(String sessionId, String recType);

    void deleteByUserIdAndRecType(Long userId, String recType);

    void deleteBySessionIdAndRecType(String sessionId, String recType);
}
