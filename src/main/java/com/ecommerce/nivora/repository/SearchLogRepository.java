package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT s.queryText, COUNT(s) as cnt FROM SearchLog s GROUP BY s.queryText ORDER BY cnt DESC")
    List<Object[]> findTopQueries(Pageable pageable);
}
