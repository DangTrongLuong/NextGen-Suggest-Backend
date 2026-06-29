package com.ecommerce.nivora.chat.entity;

import com.ecommerce.nivora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_logs", indexes = {
    @Index(name = "idx_chat_log_created", columnList = "created_at"),
    @Index(name = "idx_chat_log_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "bot_response", nullable = false, columnDefinition = "TEXT")
    private String botResponse;

    @Column(name = "intent", nullable = false, length = 50)
    private String intent;

    @Column(name = "is_out_of_scope", nullable = false)
    @Builder.Default
    private Boolean isOutOfScope = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
