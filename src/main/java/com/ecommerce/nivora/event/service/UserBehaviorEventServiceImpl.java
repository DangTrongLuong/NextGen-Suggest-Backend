package com.ecommerce.nivora.event.service;

import com.ecommerce.nivora.event.dto.request.UserBehaviorEventRequest;
import com.ecommerce.nivora.event.entity.UserBehaviorEvent;
import com.ecommerce.nivora.event.repository.UserBehaviorEventRepository;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorEventServiceImpl implements UserBehaviorEventService {

    private final UserBehaviorEventRepository behaviorEventRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    @Transactional
    public void trackEvent(UserBehaviorEventRequest request, Long userId) {
        log.info("Tracking behavior event: user={}, type={}, product={}", userId, request.getEventType(), request.getProductId());

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId()).orElse(null);
        }

        UserBehaviorEvent event = UserBehaviorEvent.builder()
                .sessionId(request.getSessionId())
                .user(user)
                .eventType(request.getEventType())
                .product(product)
                .queryText(request.getQueryText())
                .build();

        behaviorEventRepository.save(event);

        // Trigger async profile update in FastAPI backend
        String productUuid = product != null ? product.getUuid() : null;
        triggerFastApiProfileUpdate(userId, request.getSessionId(), productUuid);
    }

    private void triggerFastApiProfileUpdate(Long userId, String sessionId, String productUuid) {
        CompletableFuture.runAsync(() -> {
            try {
                // Wait briefly for the MySQL transaction to commit
                Thread.sleep(100);

                String targetUrl = fastApiUrl + "/api/v1/recommend/profile/update";
                Map<String, Object> payload = new HashMap<>();
                payload.put("user_id", userId != null ? String.valueOf(userId) : null);
                payload.put("session_id", sessionId);
                if (userId == null && productUuid != null) {
                    payload.put("guest_viewed_uuids", List.of(productUuid));
                } else {
                    payload.put("guest_viewed_uuids", Collections.emptyList());
                }

                log.info("Triggering async profile update in FastAPI for user={}, session={}", userId, sessionId);
                restTemplate.postForObject(targetUrl, payload, Map.class);
            } catch (Exception e) {
                log.error("Failed to update user profile in FastAPI asynchronously", e);
            }
        });
    }
}

