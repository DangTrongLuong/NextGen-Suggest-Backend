package com.ecommerce.nivora.event.listener;

import com.ecommerce.nivora.event.dto.FastApiProfileUpdateEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for FastApiProfileUpdateEvent and calls FastAPI AFTER the DB transaction commits.
 *
 * Key design decisions:
 * - @TransactionalEventListener(phase = AFTER_COMMIT): guarantees the event handler
 *   only fires once the MySQL transaction has fully committed — no Thread.sleep() hacks needed.
 * - @Async("asyncExecutor"): runs on our bounded thread pool, not ForkJoinPool.commonPool,
 *   preventing pool saturation when FastAPI is slow or down.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FastApiEventListener {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBehaviorEventCommitted(FastApiProfileUpdateEvent event) {
        try {
            String targetUrl = fastApiUrl + "/api/v1/recommend/profile/update";

            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", event.userId() != null ? String.valueOf(event.userId()) : null);
            payload.put("session_id", event.sessionId());

            if (event.userId() == null && event.productUuid() != null) {
                payload.put("guest_viewed_uuids", List.of(event.productUuid()));
            } else {
                payload.put("guest_viewed_uuids", Collections.emptyList());
            }

            log.info("Calling FastAPI profile update for user={}, session={}",
                    event.userId(), event.sessionId());
            restTemplate.postForObject(targetUrl, payload, Map.class);

        } catch (Exception e) {
            // Non-critical: profile updates are best-effort
            log.warn("FastAPI profile update failed for user={}, session={}: {}",
                    event.userId(), event.sessionId(), e.getMessage());
        }
    }
}
