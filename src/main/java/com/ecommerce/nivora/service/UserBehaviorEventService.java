package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.request.UserBehaviorEventRequest;

public interface UserBehaviorEventService {
    void trackEvent(UserBehaviorEventRequest request, Long userId);
}
