package com.ecommerce.nivora.search.entity;

import com.ecommerce.nivora.user.entity.User;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs", indexes = {
    @Index(name = "idx_search_log_created", columnList = "created_at"),
    @Index(name = "idx_search_log_user", columnList = "user_id"),
    @Index(name = "idx_search_logs_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_search_logs_session_created", columnList = "session_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "results_count")
    @Builder.Default
    private Integer resultsCount = 0;

    @Column(name = "latency_ms")
    @Builder.Default
    private Integer latencyMs = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
