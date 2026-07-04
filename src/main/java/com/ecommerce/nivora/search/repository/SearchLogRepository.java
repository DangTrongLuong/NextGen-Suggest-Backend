package com.ecommerce.nivora.search.repository;

import com.ecommerce.nivora.search.entity.SearchLog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT s.queryText, COUNT(s) as cnt FROM SearchLog s GROUP BY s.queryText ORDER BY cnt DESC")
    List<Object[]> findTopQueries(Pageable pageable);

    @Query("SELECT s.queryText, COUNT(s) as cnt FROM SearchLog s WHERE s.createdAt >= :since GROUP BY s.queryText ORDER BY cnt DESC")
    List<Object[]> findTopQueriesSince(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT s.queryText FROM SearchLog s WHERE s.user.id = :userId GROUP BY s.queryText ORDER BY MAX(s.createdAt) DESC")
    List<String> findRecentQueriesByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s.queryText FROM SearchLog s WHERE s.sessionId = :sessionId GROUP BY s.queryText ORDER BY MAX(s.createdAt) DESC")
    List<String> findRecentQueriesBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM SearchLog s WHERE s.user.id = :userId AND s.queryText = :queryText")
    void deleteByUserIdAndQueryText(@Param("userId") Long userId, @Param("queryText") String queryText);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM SearchLog s WHERE s.sessionId = :sessionId AND s.queryText = :queryText")
    void deleteBySessionIdAndQueryText(@Param("sessionId") String sessionId, @Param("queryText") String queryText);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM SearchLog s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM SearchLog s WHERE s.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);
}
