package com.ecommerce.nivora.event.service;

import com.ecommerce.nivora.event.dto.request.UserBehaviorEventRequest;



public interface UserBehaviorEventService {
    void trackEvent(UserBehaviorEventRequest request, Long userId);
}
