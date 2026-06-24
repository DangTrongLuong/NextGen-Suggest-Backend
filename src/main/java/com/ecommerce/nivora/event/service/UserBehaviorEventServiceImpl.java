package com.ecommerce.nivora.event.service;

import com.ecommerce.nivora.event.dto.FastApiProfileUpdateEvent;
import com.ecommerce.nivora.event.dto.request.UserBehaviorEventRequest;
import com.ecommerce.nivora.event.entity.UserBehaviorEvent;
import com.ecommerce.nivora.event.repository.UserBehaviorEventRepository;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks user behavior events.
 *
 * FastAPI profile update is triggered via ApplicationEventPublisher + @TransactionalEventListener
 * so the FastAPI call only happens AFTER the MySQL transaction commits — no Thread.sleep() needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorEventServiceImpl implements UserBehaviorEventService {

    private final UserBehaviorEventRepository behaviorEventRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void trackEvent(UserBehaviorEventRequest request, Long userId) {
        log.info("Tracking behavior event: user={}, type={}, product={}",
                userId, request.getEventType(), request.getProductId());

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

        // Publish event — FastApiEventListener will call FastAPI AFTER this transaction commits.
        // This replaces the previous CompletableFuture.runAsync() + Thread.sleep(100) approach.
        String productUuid = product != null ? product.getUuid() : null;
        eventPublisher.publishEvent(new FastApiProfileUpdateEvent(userId, request.getSessionId(), productUuid));
    }
}
