package com.ecommerce.nivora.event.dto;

/**
 * Application event published after a UserBehaviorEvent is saved.
 * Consumed by FastApiEventListener AFTER the DB transaction commits
 * via @TransactionalEventListener(phase = AFTER_COMMIT).
 */
public record FastApiProfileUpdateEvent(Long userId, String sessionId, String productUuid) {}
