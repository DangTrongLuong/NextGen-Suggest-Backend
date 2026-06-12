package com.ecommerce.nivora.recommendation.entity;

import com.ecommerce.nivora.user.entity.User;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_cache", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_rec_type", columnNames = {"user_id", "rec_type"}),
    @UniqueConstraint(name = "uq_session_rec_type", columnNames = {"session_id", "rec_type"})
}, indexes = {
    @Index(name = "idx_rec_cache_updated", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "rec_type", nullable = false, length = 50)
    private String recType;

    @Column(name = "product_ids", nullable = false, columnDefinition = "TEXT")
    private String productIds;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
