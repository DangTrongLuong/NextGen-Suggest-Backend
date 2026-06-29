package com.ecommerce.nivora.chat.repository;

import com.ecommerce.nivora.chat.entity.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
}
