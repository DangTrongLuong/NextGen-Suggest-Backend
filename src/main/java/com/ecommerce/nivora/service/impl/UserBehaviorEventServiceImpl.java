package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.request.UserBehaviorEventRequest;
import com.ecommerce.nivora.entity.Product;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.entity.UserBehaviorEvent;
import com.ecommerce.nivora.repository.ProductRepository;
import com.ecommerce.nivora.repository.UserBehaviorEventRepository;
import com.ecommerce.nivora.repository.UserRepository;
import com.ecommerce.nivora.service.UserBehaviorEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorEventServiceImpl implements UserBehaviorEventService {

    private final UserBehaviorEventRepository behaviorEventRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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
    }
}
