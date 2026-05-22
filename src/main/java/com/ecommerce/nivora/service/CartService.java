package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.request.CartItemRequest;
import com.ecommerce.nivora.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, CartItemRequest request);
    CartResponse updateItem(Long userId, Long variantId, Integer quantity);
    CartResponse removeItem(Long userId, Long variantId);
    void clearCart(Long userId);
}
